#!/bin/sh

set -e
set -o xtrace

cd "$(dirname "$0")"

HELM_CHART_VERSION="$(cat ../version.txt)"

sed -i.bak -E "s/^appVersion:.*/appVersion: \"${HELM_CHART_VERSION}\"/" modelix/Chart.yaml
sed -i.bak -E "s/^version:.*/version: \"${HELM_CHART_VERSION}\"/" modelix/Chart.yaml

mkdir -p repo
cd repo
helm package ../modelix/
helm repo index ./

curl -v --user "${ARTIFACTS_ITEMIS_CLOUD_USER}:${ARTIFACTS_ITEMIS_CLOUD_PW}" https://artifacts.itemis.cloud/repository/helm-modelix/ --upload-file modelix-${HELM_CHART_VERSION}.tgz
