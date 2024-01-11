#!/bin/sh

set -e
set -x

(
 cd db
 ./docker-build-local-and-publish-on-ci.sh
)
(
 cd keycloak-extensions
 ./docker-build-local-and-publish-on-ci.sh
)
(
 cd proxy
 ./docker-build-local-and-publish-on-ci.sh
)