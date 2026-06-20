FROM ghcr.io/jqlang/jq@sha256:b9c68867e5766576263a222e91db3de422d802069c7af70440e667a95344e486

FROM gcr.io/kaniko-project/executor:debug@sha256:2562c4fe551399514277ffff7dcca9a3b1628c4ea38cb017d7286dc6ea52f4cd

# `jq` will be used in the workspace-job.sh
COPY --from=0 /jq /busybox/jq
COPY workspace-job.sh /workspace-job.sh

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["/workspace-job.sh"]
