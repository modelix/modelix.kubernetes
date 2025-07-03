#!/usr/bin/env sh

(
  kubectl get secret test-modelix-workspace-manager-rsa-keys --namespace modelix-test -o jsonpath='{.data.private}' \
  || kubectl get secret dev-modelix-workspace-manager-rsa-keys --namespace modelix-dev -o jsonpath='{.data.private}'
) | base64 --decode > build/private-key.pem
