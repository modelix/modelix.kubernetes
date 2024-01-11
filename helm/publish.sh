#!/bin/sh

set -e
set -o xtrace

cd "$(dirname "$0")"

HELM_CHART_VERSION="$(cat ../helm-chart-version.txt)"
./update-versions.sh

mkdir -p repo
cd repo
helm package ../modelix/
helm repo index ./

curl -v --user "${ARTIFACTS_ITEMIS_CLOUD_USER}:${ARTIFACTS_ITEMIS_CLOUD_PW}" https://artifacts.itemis.cloud/repository/helm-modelix/ --upload-file modelix-${HELM_CHART_VERSION}.tgz
