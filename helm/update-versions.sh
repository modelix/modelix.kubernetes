#!/bin/sh

set -e
set -x

cd "$(dirname "$0")"

getProperty() {
   PROPERTY_KEY=$1
   PROPERTY_VALUE=$(grep "$PROPERTY_KEY" < ../versions.properties | cut -d'=' -f2)
   echo "$PROPERTY_VALUE"
}

MODELIX_CORE_VERSION=$(getProperty modelixCoreVersion)
MODELIX_WORKSPACES_VERSION=$(getProperty modelixWorkspacesVersion)
HELM_CHART_VERSION="$(cat ../helm-chart-version.txt)"

sed -i.bak -E "s/^appVersion:.*/appVersion: \"${MODELIX_WORKSPACES_VERSION}\"/" modelix/Chart.yaml
sed -i.bak -E "s/^version:.*/version: \"${HELM_CHART_VERSION}\"/" modelix/Chart.yaml
rm modelix/Chart.yaml.bak

sed -i.bak -E "s/  model: \".*\"/  model: \"${MODELIX_CORE_VERSION}\"/" modelix/values.yaml
rm modelix/values.yaml.bak

