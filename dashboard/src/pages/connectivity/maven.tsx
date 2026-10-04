import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import TableContainer from "@mui/material/TableContainer";
import Table from "@mui/material/Table";
import TableHead from "@mui/material/TableHead";
import TableBody from "@mui/material/TableBody";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import Paper from "@mui/material/Paper";
import Toolbar from "@mui/material/Toolbar";
import AddIcon from "@mui/icons-material/Add";
import Tooltip from "@mui/material/Tooltip";
import Button from "@mui/material/Button";
import { useAuth } from "react-oidc-context";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import CheckIcon from "@mui/icons-material/Check";
import { useState } from "react";
import TextField from "@mui/material/TextField";
import {
  MavenRepository,
  useDeleteMavenRepositoryMutation,
  useGetMavenConnectorConfigQuery,
  useUpdateMavenRepositoryMutation,
} from "../../api/mavenConnectorApi.ts";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";

export default function MavenConnectivityPage() {
  const auth = useAuth();
  if (auth.isAuthenticated) {
    return <MavenConnectivityPageWhenAuthenticated />;
  } else {
    return "Login required";
  }
}

function MavenConnectivityPageWhenAuthenticated() {
  const query = useGetMavenConnectorConfigQuery();
  if (query.isLoading) return "Loading...";
  if (query.isError) {
    console.log(query.error);
    return "An error has occurred: " + JSON.stringify(query.error, null, 2);
  }
  return (
    <Stack spacing={4}>
      <RepositoriesTable repositories={query.data?.repositories ?? []} />
      <Paper>
        <Toolbar sx={{ pl: 3, pr: 3 }}>
          <Typography sx={{ flex: "1 1 100%" }} component="div" variant="h5">
            Artifacts
          </Typography>
          <Tooltip title="Add New Artifact">
            <Button variant="contained" startIcon={<AddIcon />}>
              Add
            </Button>
          </Tooltip>
        </Toolbar>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Group</TableCell>
                <TableCell>Artifact</TableCell>
                <TableCell>Version</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {query.data?.artifacts?.map((artifact, index) => {
                return (
                  <TableRow key={index}>
                    <TableCell>{artifact.groupId}</TableCell>
                    <TableCell>{artifact.artifactId}</TableCell>
                    <TableCell>{artifact.version}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Stack>
  );
}

function RepositoriesTable({
  repositories,
}: {
  repositories: MavenRepository[];
}) {
  return (
    <Paper>
      <Toolbar sx={{ pl: 3, pr: 3 }}>
        <Typography sx={{ flex: "1 1 100%" }} component="div" variant="h5">
          Repositories
        </Typography>
      </Toolbar>
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>URL</TableCell>
              <TableCell></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {repositories?.map((repo, index) => {
              return <RepositoryRow key={repo.id} repo={repo} />;
            })}
            <NewRepositoryRow />
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
}

function NewRepositoryRow() {
  const [id, setId] = useState("");
  const [url, setUrl] = useState("");
  const [sendUpdate, mutationResult] = useUpdateMavenRepositoryMutation();

  function save() {
    sendUpdate({ repositoryId: id, mavenRepository: { id, url } })
      .then(() => setId(""))
      .then(() => setUrl(""));
  }

  return (
    <TableRow>
      <TableCell>
        <TextField
          fullWidth
          value={id}
          onChange={(e) => setId(e.target.value)}
        />
      </TableCell>
      <TableCell>
        <TextField
          fullWidth
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
      </TableCell>
      <TableCell align="right">
        {mutationResult.isLoading ? (
          <IconButton disabled>
            <HourglassEmptyIcon />
          </IconButton>
        ) : (
          <IconButton onClick={save}>
            <AddIcon />
          </IconButton>
        )}
      </TableCell>
    </TableRow>
  );
}

function RepositoryRow({ repo }: { repo: MavenRepository }) {
  const [editingMode, setEditingMode] = useState(false);
  const [modifiedData, setModifiedData] = useState<MavenRepository | null>(
    null,
  );
  const [sendUpdate, mutationResult] = useUpdateMavenRepositoryMutation();
  const [deleteRepository, deletionResult] = useDeleteMavenRepositoryMutation();

  function handleDelete() {
    if (repo) {
      deleteRepository({ repositoryId: repo.id });
    }
  }
  function save() {
    setEditingMode(false);
    if (modifiedData) {
      sendUpdate({
        repositoryId: modifiedData.id,
        mavenRepository: modifiedData,
      }).then(() => setModifiedData(null));
    }
  }

  return (
    <TableRow key={repo?.id ?? "new"}>
      <TableCell>{repo.id}</TableCell>
      <TableCell>
        {editingMode ? (
          <TextField
            fullWidth
            value={modifiedData?.url ?? repo.url}
            onChange={(e) =>
              setModifiedData({
                ...(modifiedData ?? repo),
                url: e.target.value,
              })
            }
          />
        ) : (
          repo.url
        )}
      </TableCell>
      <TableCell align="right">
        {mutationResult.isLoading ? (
          <IconButton disabled>
            <HourglassEmptyIcon />
          </IconButton>
        ) : editingMode ? (
          <IconButton onClick={save}>
            <CheckIcon />
          </IconButton>
        ) : (
          <IconButton onClick={() => setEditingMode(true)}>
            <EditIcon />
          </IconButton>
        )}
        {deletionResult.isLoading ? (
          <IconButton disabled>
            <HourglassEmptyIcon />
          </IconButton>
        ) : (
          <IconButton onClick={handleDelete}>
            <DeleteIcon />
          </IconButton>
        )}
      </TableCell>
    </TableRow>
  );
}
