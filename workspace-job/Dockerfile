FROM ghcr.io/jqlang/jq@sha256:848e695f66626a96c9ad527cdeddb42fcadd968ca6cc165ecf152744d297bf70

FROM gcr.io/kaniko-project/executor:debug@sha256:2562c4fe551399514277ffff7dcca9a3b1628c4ea38cb017d7286dc6ea52f4cd

# `jq` will be used in the workspace-job.sh
COPY --from=0 /jq /busybox/jq
COPY workspace-job.sh /workspace-job.sh

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["/workspace-job.sh"]
