import {useCallback, useState} from "react";
import {
  Autocomplete,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from "@mui/material";
import Box from "@mui/material/Box";
import Tooltip from "@mui/material/Tooltip";
import IconButton from "@mui/material/IconButton";
import RocketLaunchIcon from "@mui/icons-material/RocketLaunch";
import Typography from "@mui/material/Typography";
import {
  DraftConfig,
  GitRepositoryConfig,
  useCreateDraftInRepositoryMutation,
  useGetGitRepositoryQuery,
  useGetGitRepositoryStatusQuery, useListBranchesQuery,
  useListDraftsInRepositoryQuery,
  useListGitRepositoriesQuery,
} from "../../../api/gitConnectorApi.ts";
import TextField from "@mui/material/TextField";
import {
  useCreateInstanceMutation,
  useCreateWorkspaceMutation,
  useGetWorkspaceQuery,
  useListWorkspacesQuery,
  WorkspaceConfig,
} from "../../../api/workspacesApi.ts";
import Button from "@mui/material/Button";
import { useNavigate } from "react-router";

export default function WorkspaceLaunchButton(props: {
  initialGitRepositoryId?: string;
  initialGitBranchName?: string;
  initialWorkspaceId?: string;
}) {
  const [dialogOpen, setDialogOpen] = useState<boolean>(false);
  const dialogCloseCallback = useCallback(() => setDialogOpen(false), [])

  const handleButtonClick = () => {
    setDialogOpen(true);

  };

  return (
    <>
      <Tooltip title="Launch Workspace">
        <IconButton onClick={handleButtonClick}>
          <RocketLaunchIcon />
        </IconButton>
      </Tooltip>
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle variant="h4">
          Launch Workspace
        </DialogTitle>
        <WorkspaceLaunchDialogContent
          initialGitBranchName={props.initialGitBranchName}
          initialGitRepositoryId={props.initialGitRepositoryId}
          initialWorkspaceId={props.initialWorkspaceId}
          closeDialog={dialogCloseCallback}
        />
      </Dialog>
    </>
  );
}

function WorkspaceLaunchDialogContent(props: {
  initialGitRepositoryId?: string;
  initialGitBranchName?: string;
  initialWorkspaceId?: string;
  closeDialog: () => void;
}) {
  const [repositoryId, setRepositoryId] = useState<string | undefined>(
    props.initialGitRepositoryId,
  );
  const [gitBranchName, setGitBranchName] = useState<string | undefined>(
    props.initialGitBranchName,
  );
  const [draftId, setDraftId] = useState<string | undefined>();
  const [workspaceId, setWorkspaceId] = useState<string | undefined>(props.initialWorkspaceId);
  const navigate = useNavigate();
  const [
    createDraftInRepositoryMutation,
    createDraftInRepositoryMutationResult,
  ] = useCreateDraftInRepositoryMutation();
  const [newInstanceMutation, newInstanceMutationResult] =
    useCreateInstanceMutation();
  const workspaceQuery = useGetWorkspaceQuery({
    workspaceId: workspaceId ?? "",
  });
  const gitBranchQuery = useListBranchesQuery({repositoryId: repositoryId ?? ""})

  async function handleLaunch() {
    const newOrChosenDraftId =
      draftId ??
      (
        await createDraftInRepositoryMutation({
          repositoryId: repositoryId ?? "",
          draftConfig: {
            id: "",
            gitRepositoryId: repositoryId!,
            gitBranchName: gitBranchName!,
            baseGitCommit: "",
            modelixBranchName: "",
          },
        })
      )?.data?.id;
    if (newOrChosenDraftId === undefined) return;
    setDraftId(newOrChosenDraftId);

    const instanceId = (
      await newInstanceMutation({
        workspaceInstance: {
          id: "",
          config: workspaceQuery.data!,
          drafts: [newOrChosenDraftId],
          enabled: true,
          state: "CREATED",
        },
      })
    )?.data?.id;
    props.closeDialog();
    navigate("/workspaces/workspaces/" + workspaceId);
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
            Repository
          </Typography>
          <RepositoryChooser
            repositoryId={repositoryId}
            onChange={(id) => setRepositoryId(id)}
          />
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            Branch
          </Typography>
          <GitBranchChooser
            repositoryId={repositoryId ?? ""}
            branchName={gitBranchName}
            onChange={(newName) => setGitBranchName(newName)}
          />
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            Workspace
          </Typography>
          {repositoryId ? (
            <WorkspaceChooser
              repositoryId={repositoryId ?? ""}
              draftId={draftId}
              workspaceId={workspaceId}
              onChange={(id) => setWorkspaceId(id)}
            />
          ) : (
            <Typography color="textSecondary" sx={{ fontStyle: "italic" }}>
              Choose repository first
            </Typography>
          )}
          <Typography color="textSecondary" sx={{ gridColumnStart: 1 }}>
            Draft
          </Typography>
          {workspaceId ? (
            <DraftChooser
              repositoryId={repositoryId ?? ""}
              branchName={gitBranchName}
              draftId={draftId}
              onChange={(id) => setDraftId(id)}
            />
          ) : (
            <Typography color="textSecondary" sx={{ fontStyle: "italic" }}>
              Choose a workspace first
            </Typography>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button
          variant="contained"
          startIcon={<RocketLaunchIcon />}
          disabled={repositoryId === undefined || gitBranchName === undefined || workspaceId === undefined}
          onClick={handleLaunch}
        >
          Launch
        </Button>
      </DialogActions>
    </>
  );
}

export function RepositoryChooser(props: {
  repositoryId?: string;
  onChange: (newId?: string) => void;
  placeholder?: string;
}) {
  const repositoryListQuery = useListGitRepositoriesQuery({});
  const selectedRepository = repositoryListQuery.data?.repositories?.find(
    (r) => r.id === props.repositoryId,
  );
  const sortedOptions = repositoryListQuery.data?.repositories?.slice() ?? [];
  sortedOptions.sort((a, b) => (a.name ?? "").localeCompare(b.name ?? ""));
  return (
    <Autocomplete<GitRepositoryConfig>
      sx={{ minWidth: 300 }}
      value={selectedRepository ?? null}
      onChange={(e, newValue) => props.onChange(newValue?.id)}
      options={sortedOptions}
      isOptionEqualToValue={(option, value) => option.id === value.id}
      renderInput={(params) => (
        <TextField
          {...params}
          value={selectedRepository?.name ?? ""}
          placeholder={props.placeholder}
        />
      )}
      getOptionLabel={(option) => option.name ?? option.id}
    />
  );
}

function GitBranchChooser(props: {
  repositoryId: string;
  branchName?: string;
  onChange: (newName?: string) => void;
}) {
  const branchListQuery = useGetGitRepositoryStatusQuery({
    repositoryId: props.repositoryId,
  });
  const sortedOptions =
    branchListQuery.data?.branches?.map((b) => b.name) ?? [];
  sortedOptions.sort((a, b) => a.localeCompare(b));

  const selectedBranch = sortedOptions.find((b) => b === props.branchName);

  return (
    <Autocomplete<string>
      sx={{ minWidth: 500 }}
      value={selectedBranch ?? null}
      onChange={(e, newValue) => props.onChange(newValue ?? undefined)}
      options={sortedOptions}
      renderInput={(params) => (
        <TextField {...params} value={selectedBranch ?? ""} />
      )}
    />
  );
}

function DraftChooser(props: {
  repositoryId: string;
  branchName?: string;
  draftId?: string;
  onChange: (newId?: string) => void;
}) {
  const draftsQuery = useListDraftsInRepositoryQuery({
    repositoryId: props.repositoryId,
  });
  const filteredDrafts =
    draftsQuery.data?.drafts
      .filter(
        (d) =>
          props.repositoryId === undefined ||
          d.gitRepositoryId === props.repositoryId,
      )
      .filter(
        (d) =>
          props.branchName === undefined ||
          d.gitBranchName === props.branchName,
      ) ?? [];

  const selectedDraft = draftsQuery.data?.drafts.find(
    (d) => d.id === props.draftId,
  );

  return (
    <Autocomplete<DraftConfig>
      sx={{ minWidth: 300 }}
      value={selectedDraft ?? null}
      onChange={(e, newValue) => props.onChange(newValue?.id ?? undefined)}
      options={filteredDrafts}
      getOptionLabel={(option) => option.name ?? option.id}
      renderInput={(params) => (
        <TextField
          {...params}
          value={props.draftId}
          placeholder="Create New Draft"
        />
      )}
    />
  );
}

function WorkspaceChooser(props: {
  repositoryId: string;
  draftId?: string;
  workspaceId?: string;
  onChange: (newId?: string) => void;
}) {
  const repositoryQuery = useGetGitRepositoryQuery({
    repositoryId: props.repositoryId,
  });
  const workspacesQuery = useListWorkspacesQuery();
  const [createWorkspaceMutation, createWorkspaceMutationResult] =
    useCreateWorkspaceMutation();
  const navigate = useNavigate();
  const filteredWorkspaces =
    workspacesQuery.data?.workspaces?.filter((w) =>
      (w.gitRepositoryIds ?? []).includes(props.repositoryId),
    ) ?? [];

  const selectedWorkspace = filteredWorkspaces.find(
    (d) => d.id === props.workspaceId,
  );

  if (filteredWorkspaces.length === 0) {
    return (
      <Button
        variant="contained"
        onClick={async () => {
          const newWorkspace = await createWorkspaceMutation({
            workspaceConfig: {
              id: "",
              name: repositoryQuery?.data?.name ?? "",
              mpsVersion: "",
              memoryLimit: "2Gi",
              gitRepositoryIds: [props.repositoryId],
            },
          });
          const id = newWorkspace?.data?.id;
          if (id) {
            navigate("/workspaces/workspaces/" + id);
          }
        }}
      >
        Create New Workspace
      </Button>
    );
  } else {
    return (
      <Autocomplete<WorkspaceConfig>
        sx={{ minWidth: 300 }}
        value={selectedWorkspace ?? null}
        onChange={(e, newValue) => props.onChange(newValue?.id ?? undefined)}
        options={filteredWorkspaces}
        getOptionLabel={(option) => option.name ?? option.id}
        renderInput={(params) => (
          <TextField
            {...params}
            value={selectedWorkspace?.name ?? selectedWorkspace?.id ?? ""}
            placeholder="Create New Workspace"
          />
        )}
      />
    );
  }
}
