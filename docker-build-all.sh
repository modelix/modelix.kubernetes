#!/bin/sh

set -e
set -x

(
 cd db
 ./docker-build.sh
)
(
 cd keycloak-extensions
 ./docker-build.sh
)
(
 cd proxy
 ./docker-build.sh
)