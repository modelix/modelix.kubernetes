import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import Table from "@mui/material/Table";
import TableHead from "@mui/material/TableHead";
import TableBody from "@mui/material/TableBody";
import {
  DraftConfig,
  GitBranchStatusData,
  useExportDraftMutation,
  useGetDraftExportJobQuery,
  useGetDraftRebaseJobQuery,
  useListBranchesQuery,
  useListDraftsInRepositoryQuery,
  useRebaseDraftMutation,
} from "../../../api/gitConnectorApi.ts";
import IconButton from "@mui/material/IconButton";
import PublishIcon from '@mui/icons-material/Publish';
import DownloadIcon from '@mui/icons-material/Download';
import Tooltip from "@mui/material/Tooltip";

export function DraftsTable({ repositoryId }: { repositoryId: string }) {
  const draftsQuery = useListDraftsInRepositoryQuery(
    { repositoryId: repositoryId },
    { pollingInterval: 3000 },
  );
  const branchesQuery = useListBranchesQuery(
    { repositoryId: repositoryId },
    { pollingInterval: 5000 },
  );

  if (draftsQuery.isLoading) return "Loading...";
  if (draftsQuery.isError) return "Error: " + JSON.stringify(draftsQuery.error);

  const branchStatusByName = new Map<string, GitBranchStatusData>();
  branchesQuery.data?.branches.forEach((branch) =>
    branchStatusByName.set(branch.name, branch),
  );

  return (
    <Table>
      <TableHead>
        <TableRow>
          <TableCell>ID</TableCell>
          <TableCell>Branch</TableCell>
          <TableCell>Git Base Commit</TableCell>
          <TableCell align="right">Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {(draftsQuery.data?.drafts ?? []).map((draft) => (
          <DraftRow
            key={draft.id}
            draft={draft}
            branchStatus={branchStatusByName.get(draft.gitBranchName)}
          />
        ))}
      </TableBody>
    </Table>
  );
}

function DraftRow({
  draft,
  branchStatus,
}: {
  draft: DraftConfig;
  branchStatus?: GitBranchStatusData;
}) {
  const [rebaseDraftMutation, rebaseDraftMutationResult] = useRebaseDraftMutation();
  const [exportDraftMutation, exportDraftMutationResult] = useExportDraftMutation();
  const rebaseJobQuery = useGetDraftRebaseJobQuery(
    { draftId: draft.id },
    { pollingInterval: 3000 },
  );
  const exportJobQuery = useGetDraftExportJobQuery(
    { draftId: draft.id },
    { pollingInterval: 3000 },
  );
  const canRebase = draft.baseGitCommit !== branchStatus?.gitCommitHash;
  const rebaseActive = rebaseJobQuery.isLoading || rebaseJobQuery.data?.active === true;
  const exportActive = exportJobQuery.isLoading || exportJobQuery.data?.active === true;

  return (
    <TableRow>
      <TableCell>{draft.id}</TableCell>
      <TableCell>{draft.gitBranchName}</TableCell>
      <TableCell>{draft.baseGitCommit}</TableCell>
      <TableCell align="right">
        <Tooltip
          title={
            canRebase
              ? rebaseDraftMutationResult.isLoading
                ? "Rebasing..."
                : `Rebase onto ${branchStatus?.gitCommitHash}`
              : "Rebase not available. Already up to date"
          }
        >
          <IconButton
            disabled={!canRebase || rebaseActive}
            onClick={() =>
              rebaseDraftMutation({
                draftId: draft.id,
                draftRebaseJob: {
                  baseGitCommit: draft.baseGitCommit,
                },
              })
            }
          >
            <DownloadIcon />
          </IconButton>
        </Tooltip>
        <Tooltip
          title={
            exportDraftMutationResult.isLoading
              ? "Pushing..."
              : `Push changes to Git`
          }
        >
          <IconButton
            disabled={exportJobQuery.isLoading || exportActive || exportDraftMutationResult.isLoading}
            onClick={() =>
              exportDraftMutation({
                draftId: draft.id,
                draftExportJob: {},
              })
            }
          >
            <PublishIcon />
          </IconButton>
        </Tooltip>
      </TableCell>
    </TableRow>
  );
}
