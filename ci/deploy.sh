#!/bin/bash

set -euo pipefail

NAMESPACE="modelix-test"
INTERVAL=5
TIMEOUT=300
ELAPSED=0

echo "Starting 'skaffold deploy' in background..."
skaffold deploy --filename='skaffold-test.yaml' --build-artifacts=tags.json --verbosity trace &
DEPLOY_PID=$!

echo "Watching deployment status in namespace '$NAMESPACE' (timeout: $TIMEOUT seconds)..."

while kill -0 "$DEPLOY_PID" 2>/dev/null; do
    echo ""
    echo "=== $(date): Deployment Status ==="
    kubectl get deployments -n "$NAMESPACE" -o wide || true
    echo "---"
    kubectl get pods -n "$NAMESPACE" -o wide || true

    sleep "$INTERVAL"
    ELAPSED=$((ELAPSED + INTERVAL))

    if [[ "$ELAPSED" -ge "$TIMEOUT" ]]; then
        echo "❌ Timeout after $TIMEOUT seconds. Killing Skaffold..."
        kill "$DEPLOY_PID"
        wait "$DEPLOY_PID" 2>/dev/null || true
        exit 1
    fi
done

# Wait for skaffold to finish and return its exit code
wait "$DEPLOY_PID"
SKAFFOLD_EXIT=$?

if [[ $SKAFFOLD_EXIT -eq 0 ]]; then
    echo "✅ Skaffold deploy completed successfully."
else
    echo "❌ Skaffold deploy failed with exit code $SKAFFOLD_EXIT"
fi

exit $SKAFFOLD_EXIT
