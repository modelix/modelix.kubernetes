#!/bin/sh

set -e

cd "$(dirname "$0")"

# See https://helm.sh/docs/chart_template_guide/debugging/
helm install dev ./modelix -f dev.yaml --dry-run --debug