#!/bin/sh

set -e
set -x

./db/docker-build-local-and-publish-on-ci.sh
./keycloak-extensions/docker-build-local-and-publish-on-ci.sh
./proxy/docker-build-local-and-publish-on-ci.sh