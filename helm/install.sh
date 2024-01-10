#!/bin/sh

set -e

cd "$(dirname "$0")"

helm install dev ./modelix -f dev.yaml