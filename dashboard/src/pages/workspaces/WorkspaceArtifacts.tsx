import { Fragment, ReactNode, useState } from "react";
import {
  useCreateArtifactUploadTokenMutation,
  useDeleteArtifactMutation,
  useListArtifactsQuery,
  WorkspaceArtifact,
} from "../../api/workspacesApi.ts";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import Typography from "@mui/material/Typography";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Dialog from "@mui/material/Dialog";
import DialogTitle from "@mui/material/DialogTitle";
import DialogContent from "@mui/material/DialogContent";
import DialogActions from "@mui/material/DialogActions";
import { CircularProgress } from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import ErrorIcon from "@mui/icons-material/Error";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import KeyIcon from "@mui/icons-material/Key";

/**
 * Results of external (CI) builds that were uploaded with the Gradle plugin org.modelix.workspaces.
 */
export default function WorkspaceArtifacts({
  workspaceId,
}: {
  workspaceId: string;
}): ReactNode {
  const [tokenDialogOpen, setTokenDialogOpen] = useState(false);
  return (
    <>
      <CardHeader
        title="Artifacts"
        subheader="Uploaded by CI pipelines using the Gradle plugin org.modelix.workspaces"
        slotProps={{
          title: { variant: "h6" },
          subheader: { variant: "body2" },
        }}
        action={
          <Button startIcon={<KeyIcon />} onClick={() => setTokenDialogOpen(true)}>
            Upload Token
          </Button>
        }
      />
      <ArtifactList workspaceId={workspaceId} />
      <UploadTokenDialog
        workspaceId={workspaceId}
        open={tokenDialogOpen}
        onClose={() => setTokenDialogOpen(false)}
      />
    </>
  );
}

function ArtifactList({ workspaceId }: { workspaceId: string }): ReactNode {
  const artifactsQuery = useListArtifactsQuery(
    { workspaceId: workspaceId },
    { pollingInterval: 10000 },
  );
  const [deleteArtifact, deleteResult] = useDeleteArtifactMutation();

  if (artifactsQuery.isLoading) return <CircularProgress />;
  if (artifactsQuery.isError) {
    return (
      <Tooltip title={JSON.stringify(artifactsQuery.error, null, 2)}>
        <ErrorIcon />
      </Tooltip>
    );
  }

  const artifacts = artifactsQuery.data?.artifacts ?? [];
  if (artifacts.length === 0) {
    return (
      <CardContent>
        <Typography color="textSecondary">
          No artifacts uploaded yet. Instances will start as soon as the first
          one is available.
        </Typography>
      </CardContent>
    );
  }

  return (
    <CardContent
      sx={{
        display: "grid",
        gridTemplateColumns: "max-content 1fr max-content max-content max-content",
        columnGap: 3,
        rowGap: 1,
        alignItems: "center",
      }}
    >
      {artifacts.map((artifact) => (
        <Fragment key={artifact.id}>
          <Typography>{new Date(artifact.createdAt).toLocaleString()}</Typography>
          <Tooltip title={artifactDetails(artifact)}>
            <Typography>
              {artifact.label ?? artifact.id}
              {artifact.gitBranch && ` (${artifact.gitBranch})`}
            </Typography>
          </Tooltip>
          <Typography color="textSecondary" fontSize="small">
            {artifact.gitCommit?.substring(0, 10)}
          </Typography>
          <Typography color="textSecondary" fontSize="small">
            {formatSize(artifact.sizeBytes)}
          </Typography>
          <IconButton
            disabled={deleteResult.isLoading}
            onClick={() =>
              deleteArtifact({ workspaceId: workspaceId, artifactId: artifact.id })
            }
          >
            <DeleteIcon />
          </IconButton>
        </Fragment>
      ))}
    </CardContent>
  );
}

function UploadTokenDialog({
  workspaceId,
  open,
  onClose,
}: {
  workspaceId: string;
  open: boolean;
  onClose: () => void;
}): ReactNode {
  const [validityDays, setValidityDays] = useState(90);
  const [createToken, tokenResult] = useCreateArtifactUploadTokenMutation();

  function close() {
    tokenResult.reset();
    onClose();
  }

  return (
    <Dialog open={open} onClose={close} maxWidth="md" fullWidth>
      <DialogTitle>Artifact Upload Token</DialogTitle>
      <DialogContent>
        <Typography sx={{ mb: 2 }}>
          The token only allows uploading artifacts for this workspace. Store it
          as a secret in your CI system and provide it to the Gradle plugin as
          the environment variable MODELIX_ACCESS_TOKEN.
        </Typography>
        {tokenResult.data ? (
          <>
            <TextField
              fullWidth
              multiline
              label="Token"
              value={tokenResult.data.token}
              slotProps={{
                input: {
                  readOnly: true,
                  endAdornment: (
                    <IconButton
                      onClick={() =>
                        navigator.clipboard.writeText(tokenResult.data!.token)
                      }
                    >
                      <ContentCopyIcon />
                    </IconButton>
                  ),
                },
              }}
            />
            <Typography color="textSecondary" sx={{ mt: 1 }}>
              Expires: {new Date(tokenResult.data.expiresAt).toLocaleString()}.
              The token isn't shown again.
            </Typography>
          </>
        ) : (
          <TextField
            type="number"
            label="Validity in days (max. 365)"
            value={validityDays}
            onChange={(e) => setValidityDays(Number(e.target.value))}
          />
        )}
        {tokenResult.isError && (
          <Typography color="error" sx={{ mt: 1 }}>
            {JSON.stringify(tokenResult.error)}
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        {!tokenResult.data && (
          <Button
            disabled={tokenResult.isLoading}
            onClick={() =>
              createToken({
                workspaceId: workspaceId,
                artifactUploadTokenRequest: { validityDays: validityDays },
              })
            }
          >
            Create
          </Button>
        )}
        <Button onClick={close}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

function artifactDetails(artifact: WorkspaceArtifact): string {
  return [
    `ID: ${artifact.id}`,
    artifact.gitCommit && `Commit: ${artifact.gitCommit}`,
    artifact.mpsVersion && `MPS: ${artifact.mpsVersion}`,
    artifact.uploadedBy && `Uploaded by: ${artifact.uploadedBy}`,
    `SHA-256: ${artifact.sha256}`,
  ]
    .filter(Boolean)
    .join("\n");
}

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KiB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MiB`;
}
