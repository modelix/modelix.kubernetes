import { useState } from "react";
import { useNavigate } from "react-router";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import AddIcon from "@mui/icons-material/Add";
import {
  useCreateWorkspaceMutation,
  WorkspaceBuildMode,
} from "../../api/workspacesApi.ts";
import { RepositoryChooser } from "../connectivity/git/WorkspaceLaunchButton.tsx";
import { MPS_VERSIONS } from "./mpsVersions.ts";

/**
 * Creates a workspace that isn't necessarily backed by a git repository,
 * e.g. for workspaces that run the result of an external CI build.
 */
export default function CreateWorkspaceButton() {
  const [dialogOpen, setDialogOpen] = useState(false);
  return (
    <>
      <Button
        variant="contained"
        startIcon={<AddIcon />}
        onClick={() => setDialogOpen(true)}
      >
        New Workspace
      </Button>
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle variant="h4">New Workspace</DialogTitle>
        {dialogOpen && (
          <CreateWorkspaceDialogContent closeDialog={() => setDialogOpen(false)} />
        )}
      </Dialog>
    </>
  );
}

function CreateWorkspaceDialogContent(props: { closeDialog: () => void }) {
  const [name, setName] = useState("");
  const [mpsVersion, setMpsVersion] = useState("2024.1");
  const [buildMode, setBuildMode] = useState<WorkspaceBuildMode>("IN_CLUSTER");
  const [repositoryId, setRepositoryId] = useState<string | undefined>();
  const [createWorkspaceMutation, createWorkspaceMutationResult] =
    useCreateWorkspaceMutation();
  const navigate = useNavigate();

  async function handleCreate() {
    const newWorkspace = await createWorkspaceMutation({
      workspaceConfig: {
        id: "",
        name: name.trim(),
        mpsVersion: mpsVersion,
        memoryLimit: "2Gi",
        buildMode: buildMode,
        gitRepositoryIds: repositoryId ? [repositoryId] : [],
      },
    });
    const id = newWorkspace?.data?.id;
    if (id) {
      props.closeDialog();
      navigate("/workspaces/workspaces/" + id);
    }
  }

  return (
    <>
      <DialogContent>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "max-content 1fr",
            columnGap: 3,
            rowGap: 1,
            alignItems: "center",
          }}
        >
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            Name
          </Typography>
          <TextField
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            MPS Version
          </Typography>
          <Select
            value={mpsVersion}
            onChange={(e) => setMpsVersion(e.target.value)}
          >
            {MPS_VERSIONS.map((v) => (
              <MenuItem key={v} value={v}>
                {v}
              </MenuItem>
            ))}
          </Select>
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            Build
          </Typography>
          <Select
            value={buildMode}
            onChange={(e) => setBuildMode(e.target.value as WorkspaceBuildMode)}
          >
            <MenuItem value="IN_CLUSTER">Inside the cluster</MenuItem>
            <MenuItem value="EXTERNAL">External (CI pipeline)</MenuItem>
          </Select>
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            Git Repository
          </Typography>
          <RepositoryChooser
            repositoryId={repositoryId}
            onChange={(id) => setRepositoryId(id)}
            placeholder="None"
          />
        </Box>
        {createWorkspaceMutationResult.isError && (
          <Typography color="error" sx={{ mt: 2 }}>
            Creating the workspace failed:{" "}
            {JSON.stringify(createWorkspaceMutationResult.error)}
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={props.closeDialog}>Cancel</Button>
        <Button
          variant="contained"
          disabled={
            name.trim() === "" || createWorkspaceMutationResult.isLoading
          }
          onClick={handleCreate}
        >
          Create
        </Button>
      </DialogActions>
    </>
  );
}
