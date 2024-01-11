#!/bin/sh

set -e
set -x

HELM_CHART_VERSION="$(cat ../helm-chart-version.txt)"

if [ "${CI}" = "true" ]; then
  docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --push \
    -t "modelix/modelix-proxy:${HELM_CHART_VERSION}" .
else
  docker build \
    -t "modelix/modelix-proxy:${HELM_CHART_VERSION}" .
fi