#!/bin/sh

set -e

cd "$(dirname "$0")"

helm upgrade dev ./modelix -f dev.yaml