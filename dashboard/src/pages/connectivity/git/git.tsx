import {
  GitRepositoryConfig,
  useCreateGitRepositoryMutation,
  useListGitRepositoriesQuery,
} from "../../../api/gitConnectorApi.ts";
import { useState } from "react";
import Grid from "@mui/material/Grid";
import { RepositoryComponent } from "./RepositoryCard.tsx";

export default function GitConnectivityPage() {
  const repositoriesQuery = useListGitRepositoriesQuery({}, {pollingInterval: 3000});
  const [newRepositoryMutation, newRepositoryMutationStatus] = useCreateGitRepositoryMutation();
  const newRepo = useState<GitRepositoryConfig | null>(null);

  if (repositoriesQuery.isLoading) return "Loading...";
  if (repositoriesQuery.isError)
    return "Error: " + JSON.stringify(repositoriesQuery.error);

  return (
    <Grid container spacing={2} columns={{ md: 8, lg: 12 }}>
      {repositoriesQuery.data?.repositories?.map((repo) => (
        <RepositoryComponent key={repo.id} repo={repo} />
      ))}
      <RepositoryComponent repo={null} />
    </Grid>
  );
}
