import {
  GitRemoteConfig,
  GitRepositoryConfig,
  useCreateGitRepositoryMutation,
  useDeleteGitRepositoryMutation,
  useUpdateBranchesMutation,
  useUpdateGitRepositoryMutation,
} from "../../../api/gitConnectorApi.ts";
import {Fragment, useState} from "react";
import Grid from "@mui/material/Grid";
import Card from "@mui/material/Card";
import CardHeader from "@mui/material/CardHeader";
import Button from "@mui/material/Button";
import CheckIcon from "@mui/icons-material/Check";
import IconButton from "@mui/material/IconButton";
import SyncIcon from "@mui/icons-material/Sync";
import DeleteIcon from "@mui/icons-material/Delete";
import CardContent from "@mui/material/CardContent";
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import {Accordion, AccordionActions, AccordionDetails, AccordionSummary} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import ListEditor from "../../../components/ListEditor.tsx";
import EditIcon from "@mui/icons-material/Edit";
import LockOutlineIcon from "@mui/icons-material/LockOutline";
import CancelIcon from "@mui/icons-material/Cancel";
import {BranchesTable} from "./BranchesTable.tsx";
import {DraftsTable} from "./DraftsTable.tsx";

const newRepositoryTemplate: GitRepositoryConfig = {
  id: "New Repository",
};

export function RepositoryComponent({repo}: { repo: GitRepositoryConfig | null }) {
  const [modifiedData, setModifiedData] = useState<GitRepositoryConfig | null>(null);
  const [createRepoMutation, createRepoMutationStatus] = useCreateGitRepositoryMutation();
  const [updateRepoMutation, updateRepoMutationStatus] = useUpdateGitRepositoryMutation();
  const [deleteRepoMutation, deleteRepoMutationStatus] = useDeleteGitRepositoryMutation();
  const [updateBranchesMutation, updateBranchesMutationStatus] = useUpdateBranchesMutation();

  const dataToShow = modifiedData ?? repo ?? newRepositoryTemplate;
  const isNewRepo = repo === null;
  const isEditMode = modifiedData !== null || repo === null;

  function handleSave() {
    if (isNewRepo) {
      createRepoMutation({gitRepositoryConfig: dataToShow}).then(() =>
          setModifiedData(null),
      );
    } else {
      updateRepoMutation({
        repositoryId: repo.id,
        gitRepositoryConfig: dataToShow,
      }).then(() => setModifiedData(null));
    }
  }

  function handleDelete() {
    if (repo !== null) {
      deleteRepoMutation({repositoryId: repo.id});
    }
  }

  function refreshBranches() {
    if (repo !== null) {
      updateBranchesMutation({repositoryId: repo.id});
    }
  }

  return (
      <Grid size={12}>
        <Card>
          <CardHeader
              slotProps={{
                title: {color: isNewRepo ? "textSecondary" : "inherit"},
              }}
              title={repo === null ? "New Repository" : repo.name}
              action={
                <>
                  {modifiedData !== null && (
                      <Button
                          onClick={handleSave}
                          variant="contained"
                          color="primary"
                          startIcon={<CheckIcon/>}
                      >
                        Save
                      </Button>
                  )}
                  {repo !== null && (
                      <IconButton onClick={handleDelete}>
                        <DeleteIcon/>
                      </IconButton>
                  )}
                </>
              }
          />
          <CardContent>
            <Box
                sx={{
                  display: "grid",
                  gridTemplateColumns: "max-content 1fr",
                  columnGap: 3,
                  rowGap: 3,
                  alignItems: "center",
                }}
            >
              <TextField
                  sx={{gridColumnStart: 2}}
                  label="Name"
                  value={dataToShow.name ?? ""}
                  onChange={(e) =>
                      setModifiedData({...dataToShow, name: e.target.value ?? ""})
                  }
              />
            </Box>
          </CardContent>
          <CardContent>
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                <Stack direction="row" spacing={2}>
                  <Typography>Remotes</Typography>
                  <Typography color="secondary">
                    {repo?.remotes?.at(0)?.url}
                  </Typography>
                </Stack>
              </AccordionSummary>
              <AccordionDetails>
                <Box
                    sx={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr 1fr 1fr max-content",
                      columnGap: 1,
                      rowGap: 1,
                      alignItems: "center",
                    }}
                >
                  <ListEditor<GitRemoteConfig>
                      initialData={dataToShow.remotes ?? []}
                      newItemTemplate={{
                        name: "origin",
                        url: "",
                        hasCredentials: false,
                      }}
                      onChange={(it) =>
                          setModifiedData({...dataToShow, remotes: it})
                      }
                      itemComponent={(
                          element,
                          index,
                          isNewItem,
                          deleteItem,
                          updateItem,
                      ) => (
                          <Fragment key={index}>
                            <TextField
                                sx={{gridColumnStart: 1, gridColumnEnd: 4}}
                                label={isNewItem ? "New URL" : "URL"}
                                value={element.url}
                                onChange={(e) =>
                                    updateItem({...element, url: e.target.value})
                                }
                            />
                            <TextField
                                sx={{gridColumnStart: 4, gridColumnEnd: 5}}
                                label="Name"
                                value={element.name}
                                onChange={(e) =>
                                    updateItem({...element, name: e.target.value})
                                }
                            />
                            {!isNewItem && (
                                <IconButton onClick={deleteItem}>
                                  <DeleteIcon/>
                                </IconButton>
                            )}
                            {element.url ||
                            element.hasCredentials ||
                            element.credentials ? (
                                <>
                                  {element.credentials ? (
                                      <>
                                        <TextField
                                            sx={{gridColumnStart: 1, gridColumnEnd: 3}}
                                            label="Username"
                                            value={element.credentials?.username ?? ""}
                                            onChange={(e) =>
                                                updateItem({
                                                  ...element,
                                                  hasCredentials: true,
                                                  credentials: {
                                                    username: e.target.value,
                                                    password:
                                                        element.credentials?.password ?? "",
                                                  },
                                                })
                                            }
                                        />
                                        <TextField
                                            sx={{gridColumnStart: 3, gridColumnEnd: 5}}
                                            type="password"
                                            label="Password / Token"
                                            value={element.credentials?.password ?? ""}
                                            onChange={(e) =>
                                                updateItem({
                                                  ...element,
                                                  hasCredentials: true,
                                                  credentials: {
                                                    username:
                                                        element.credentials?.username ?? "",
                                                    password: e.target.value,
                                                  },
                                                })
                                            }
                                        />
                                      </>
                                  ) : (
                                      <Button
                                          startIcon={
                                            element.hasCredentials ? (
                                                <EditIcon/>
                                            ) : (
                                                <LockOutlineIcon/>
                                            )
                                          }
                                          sx={{gridColumnStart: 1, gridColumnEnd: 5}}
                                          onClick={() =>
                                              updateItem({
                                                ...element,
                                                hasCredentials: true,
                                                credentials: {
                                                  username: "",
                                                  password: "",
                                                },
                                              })
                                          }
                                      >
                                        {element.hasCredentials
                                            ? "Change Credentials"
                                            : "Add Credentials"}
                                      </Button>
                                  )}
                                  {(element.hasCredentials || element.credentials) && (
                                      <IconButton
                                          sx={{gridColumnStart: 5, gridColumnEnd: 6}}
                                          onClick={() =>
                                              updateItem({
                                                ...element,
                                                hasCredentials: false,
                                                credentials: undefined,
                                              })
                                          }
                                      >
                                        <CancelIcon/>
                                      </IconButton>
                                  )}
                                </>
                            ) : null}
                          </Fragment>
                      )}
                  />
                </Box>
              </AccordionDetails>
            </Accordion>
            {repo && (
                <>
                  <Accordion slotProps={{transition: {unmountOnExit: true}}}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon/>}>
                      <Typography>Branches</Typography>
                    </AccordionSummary>
                    {(repo?.remotes?.length ?? 0) > 0 && (
                        <AccordionActions>
                          <IconButton disabled={updateBranchesMutationStatus.isLoading} onClick={refreshBranches}>
                            <SyncIcon/>
                          </IconButton>
                        </AccordionActions>
                    )}
                    <AccordionDetails>
                      <BranchesTable repositoryId={repo?.id}/>
                    </AccordionDetails>
                  </Accordion>
                  <Accordion slotProps={{transition: {unmountOnExit: true}}}>
                    <AccordionSummary><Typography>Drafts</Typography></AccordionSummary>
                    <AccordionDetails>
                      <DraftsTable repositoryId={repo?.id}/>
                    </AccordionDetails>
                  </Accordion>
                </>
            )}
          </CardContent>
        </Card>
      </Grid>
  );
}