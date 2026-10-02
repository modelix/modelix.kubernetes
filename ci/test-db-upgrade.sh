#!/bin/bash

# Verifies that an existing deployment of an older release of the Helm chart can be upgraded to the chart in this
# working tree without losing the content of the database. This includes major version upgrades of PostgreSQL.
#
# Usage: ./ci/test-db-upgrade.sh [old chart git ref]
#
# Requires kubectl access to a cluster with a default storage class (e.g. kind).

set -euo pipefail

cd "$(dirname "$0")/.."

OLD_CHART_REF="${1:-1.2.4}"
NAMESPACE="${NAMESPACE:-modelix-db-upgrade-test}"
RELEASE="dbtest"
DEPLOYMENT="$RELEASE-modelix-db"
TIMEOUT="${TIMEOUT:-600s}"
WORK_DIR="$(mktemp -d)"

cleanup() {
  local exit_code=$?
  if [[ $exit_code -ne 0 ]]; then
    echo "❌ Test failed. Diagnostic output:"
    kubectl -n "$NAMESPACE" get pods -o wide || true
    kubectl -n "$NAMESPACE" describe pods -l component=db || true
    kubectl -n "$NAMESPACE" logs "deployment/$DEPLOYMENT" -c db-upgrade --tail=200 || true
    kubectl -n "$NAMESPACE" logs "deployment/$DEPLOYMENT" -c db --tail=200 || true
  fi
  if [[ "${KEEP_NAMESPACE:-false}" != "true" ]]; then
    helm uninstall "$RELEASE" -n "$NAMESPACE" --wait >/dev/null 2>&1 || true
    kubectl delete namespace "$NAMESPACE" --wait=false >/dev/null 2>&1 || true
  fi
  rm -rf "$WORK_DIR"
  exit $exit_code
}
trap cleanup EXIT

fail() {
  echo "❌ $*"
  exit 1
}

psql_exec() {
  kubectl -n "$NAMESPACE" exec "deployment/$DEPLOYMENT" -c db -- psql -U modelix -d modelix -v ON_ERROR_STOP=1 -qtA -c "$1"
}

wait_for_db() {
  kubectl -n "$NAMESPACE" rollout status "deployment/$DEPLOYMENT" --timeout="$TIMEOUT"
  # The deployment has no readiness probe. Wait until the server accepts connections.
  for _ in $(seq 1 60); do
    if kubectl -n "$NAMESPACE" exec "deployment/$DEPLOYMENT" -c db -- pg_isready -U modelix -d modelix -h localhost >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  fail "Database didn't become ready"
}

upgrade_container_logs() {
  # Not piped directly into `grep -q`, which would fail the pipeline with SIGPIPE
  kubectl -n "$NAMESPACE" logs "deployment/$DEPLOYMENT" -c db-upgrade
}

server_major_version() {
  psql_exec "SELECT current_setting('server_version_num')::int / 10000"
}

# The test data is written to a separate schema to not depend on the schema created by the model server.
# The table has the same structure as the table used by the model server.
TEST_TABLE="upgrade_test.model"

# Fingerprint of the whole content of the table. Changes if any row is lost or modified.
table_fingerprint() {
  psql_exec "SELECT count(*) || ':' || md5(string_agg(key || '=' || coalesce(value, ''), ',' ORDER BY key COLLATE \"C\")) FROM $TEST_TABLE"
}

# Verifies the structure of all B-tree indexes in the database. Detects indexes that are sorted differently than the collation of the
# running server, which happens when the data is upgraded with a different C library (e.g. musl vs. glibc).
check_indexes() {
  psql_exec "CREATE EXTENSION IF NOT EXISTS amcheck"
  psql_exec "SELECT bt_index_parent_check(c.oid, true)
             FROM pg_index i
             JOIN pg_class c ON c.oid = i.indexrelid
             JOIN pg_namespace n ON n.oid = c.relnamespace
             JOIN pg_am am ON am.oid = c.relam
             WHERE am.amname = 'btree' AND c.relpersistence <> 't' AND i.indisready AND i.indisvalid" >/dev/null
  # Lookups through the primary key have to find every row
  local not_found
  not_found="$(psql_exec "SET enable_seqscan = off; SET enable_bitmapscan = off;
    SELECT count(*) FROM (SELECT key FROM $TEST_TABLE ORDER BY key COLLATE \"C\") k
    WHERE NOT EXISTS (SELECT 1 FROM $TEST_TABLE m WHERE m.key = k.key)" | tail -n 1)"
  [[ "$not_found" == "0" ]] || fail "$not_found rows can't be found through the primary key index"
}

echo "=== Installing chart from $OLD_CHART_REF ==="
git rev-parse --verify --quiet "$OLD_CHART_REF^{commit}" >/dev/null \
  || git fetch --depth=1 origin "refs/tags/$OLD_CHART_REF:refs/tags/$OLD_CHART_REF"
git archive "$OLD_CHART_REF" helm/modelix | tar -x -C "$WORK_DIR"
kubectl create namespace "$NAMESPACE"
helm install "$RELEASE" "$WORK_DIR/helm/modelix" -n "$NAMESPACE"
wait_for_db
OLD_VERSION="$(server_major_version)"
echo "Old chart runs PostgreSQL $OLD_VERSION"

echo "=== Writing test data ==="
# Keys similar to the hashes stored by the model server. Mixed case and special characters are sorted differently
# by different collation implementations.
psql_exec "CREATE SCHEMA upgrade_test"
psql_exec "CREATE TABLE $TEST_TABLE (key varchar NOT NULL, value varchar, reachable boolean, CONSTRAINT upgrade_test_pkey PRIMARY KEY (key))"
psql_exec "INSERT INTO $TEST_TABLE(key, value)
           SELECT translate(encode(sha256(i::text::bytea), 'base64'), '+/=', '-_*'), 'value-' || i
           FROM generate_series(1, 20000) i"
psql_exec "INSERT INTO $TEST_TABLE(key, value) VALUES ('canary', 'written by $OLD_CHART_REF')"
FINGERPRINT_BEFORE="$(table_fingerprint)"
echo "Fingerprint before upgrade: $FINGERPRINT_BEFORE"

echo "=== Upgrading to the current chart ==="
helm upgrade "$RELEASE" helm/modelix -n "$NAMESPACE"
wait_for_db
NEW_VERSION="$(server_major_version)"
EXPECTED_VERSION="$(helm template "$RELEASE" helm/modelix --show-only templates/local/db-deployment.yaml \
  | grep -E '^\s+image: "[^"]*postgres:' | sed -E 's/.*postgres:([0-9]+).*/\1/')"
echo "New chart runs PostgreSQL $NEW_VERSION (expected $EXPECTED_VERSION)"
[[ "$NEW_VERSION" == "$EXPECTED_VERSION" ]] || fail "Expected PostgreSQL $EXPECTED_VERSION, but got $NEW_VERSION"

if [[ "$OLD_VERSION" != "$NEW_VERSION" ]]; then
  grep -q "Upgrade to PostgreSQL .* complete" <<< "$(upgrade_container_logs)" \
    || fail "The init container didn't upgrade the database"
fi

FINGERPRINT_AFTER="$(table_fingerprint)"
echo "Fingerprint after upgrade:  $FINGERPRINT_AFTER"
[[ "$FINGERPRINT_BEFORE" == "$FINGERPRINT_AFTER" ]] || fail "Database content changed during the upgrade"
check_indexes
psql_exec "INSERT INTO $TEST_TABLE(key, value) VALUES ('written-after-upgrade', 'value')"

echo "=== Restarting the upgraded database ==="
kubectl -n "$NAMESPACE" rollout restart "deployment/$DEPLOYMENT"
wait_for_db
grep -q "Nothing to upgrade" <<< "$(upgrade_container_logs)" \
  || fail "The init container should not do anything if the database is already upgraded"
[[ "$(psql_exec "SELECT value FROM $TEST_TABLE WHERE key = 'canary'")" == "written by $OLD_CHART_REF" ]] \
  || fail "Canary row is missing after restart"

echo "✅ Upgrade from $OLD_CHART_REF (PostgreSQL $OLD_VERSION) to the current chart (PostgreSQL $NEW_VERSION) succeeded"
