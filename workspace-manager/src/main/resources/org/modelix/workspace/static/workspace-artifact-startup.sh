#!/bin/sh

# Installs the artifact of an externally built workspace into the modelix/mps-vnc-baseimage.
# Executed through PRE_STARTUP_SCRIPT_URL before MPS is started.
#
# Expected environment variables (set by the workspace-manager):
#   MODELIX_WORKSPACE_ARTIFACT_URL  URL of the artifact ZIP file
#   INITIAL_JWT_TOKEN               token with read permission for the artifact
#   MODELIX_PLUGINS_BASE_URL        URL prefix for downloading the modelix MPS plugins
#   MODELIX_PLUGINS                 space separated list of plugin ZIP file names
#   MPS_MAX_HEAP_SIZE_MB            (optional) maximum heap size of MPS

MARKER_FILE=/config/home/.modelix-artifact-installed
WORK_DIR=/tmp/modelix-artifact

if [ -f "$MARKER_FILE" ]; then
  echo "Artifact already installed"
  exit 0
fi

fail() {
  echo "### FAILED workspace-artifact-install: $1 ###"
  exit 1
}

echo "### START workspace-artifact-install ###"

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/content" || fail "Cannot create $WORK_DIR"
cd "$WORK_DIR" || fail "Cannot enter $WORK_DIR"

echo "Downloading $MODELIX_WORKSPACE_ARTIFACT_URL"
curl --fail --silent --show-error --location --retry 5 --retry-delay 3 --retry-all-errors \
  -H "Authorization: Bearer $INITIAL_JWT_TOKEN" \
  -o artifact.zip "$MODELIX_WORKSPACE_ARTIFACT_URL" || fail "Download of the artifact failed"

unzip -q artifact.zip -d content || fail "Extracting the artifact failed"
rm artifact.zip

if [ -d content/mps-projects ]; then
  # Only the projects of the artifact should be opened
  rm -rf /mps-projects/default-mps-project
  for project in content/mps-projects/*; do
    [ -e "$project" ] || continue
    rm -rf "/mps-projects/$(basename "$project")"
    mv "$project" /mps-projects/ || fail "Cannot install project $project"
  done
fi

if [ -d content/mps-languages ]; then
  cp -R content/mps-languages/. /mps-languages/ || fail "Cannot install languages"
fi

if [ -d content/mps-plugins ]; then
  cp -R content/mps-plugins/. /mps-plugins/ || fail "Cannot copy plugins"
fi

for plugin in $MODELIX_PLUGINS; do
  echo "Downloading $MODELIX_PLUGINS_BASE_URL$plugin"
  curl --fail --silent --show-error --location --retry 5 --retry-delay 3 --retry-all-errors \
    -o "/mps-plugins/$plugin" "$MODELIX_PLUGINS_BASE_URL$plugin" || fail "Download of $plugin failed"
done

/install-plugins.sh /mps/plugins/ /mps-plugins/ 1 || fail "Installing plugins failed"

# The model sync should only connect to the repository of the current workspace
find /mps-projects/ -path '*/.mps/cloudResources.xml' -delete

if [ -n "$MPS_MAX_HEAP_SIZE_MB" ]; then
  sed -i.bak '/-Xmx/d' /mps/bin/mps64.vmoptions \
    && sed -i.bak '/-XX:MaxRAMPercentage/d' /mps/bin/mps64.vmoptions \
    && echo "-Xmx${MPS_MAX_HEAP_SIZE_MB}m" >> /mps/bin/mps64.vmoptions \
    && cat /mps/bin/mps64.vmoptions > /mps/bin/mps.vmoptions
fi

cd /
rm -rf "$WORK_DIR"
touch "$MARKER_FILE"

echo "### DONE workspace-artifact-install ###"
