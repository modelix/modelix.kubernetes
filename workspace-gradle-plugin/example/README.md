# Example: publishing an MPS project to a Modelix workspace

A small MPS 2024.1 project (from [git-import-test-repo](https://github.com/slisson/git-import-test-repo))
built the way a CI pipeline would build it:

1. `org.modelix.mps.build-tools` downloads MPS, then generates and compiles the modules in place
   (task `assembleMpsModules`, requires `ant` on the `PATH`).
2. `org.modelix.workspaces` packages the `mps` folder and uploads it (task `publishModelixWorkspaceArtifact`).

The workspaces plugin is taken from the sources of this repository (`includeBuild("../..")` in `settings.gradle.kts`).

## Running it against a local cluster

1. Open the dashboard (http://localhost/modelix/dashboard/), create a workspace with MPS 2024.1 and select the
   build mode "External (CI pipeline)". The workspace ID is shown in the URL.
2. Create an upload token in the artifacts section of the workspace ("Upload Token"), or with the REST API:

   ```shell
   curl -X POST -H "Authorization: Bearer $YOUR_TOKEN" -H "Content-Type: application/json" \
     -d '{"validityDays": 1}' \
     http://localhost/modelix/workspaces/workspaces/<workspace-id>/artifact-upload-token
   ```

3. Build and upload:

   ```shell
   MODELIX_ACCESS_TOKEN=<token> ./gradlew publishModelixWorkspaceArtifact -Pmodelix.workspaceId=<workspace-id>
   ```

4. Start an instance of the workspace. MPS opens the project `git-import-test-repo`.

The cluster is accessed with plain HTTP, because the self-signed certificate of a local cluster isn't trusted by Java.
Use `-Pmodelix.serverUrl=...` for another cluster.

The git commit and branch in the manifest are the ones of the modelix.kubernetes repository.
