# Modelix Workspaces Gradle Plugin

Publishes the result of a CI build of an MPS project to a Modelix workspace.
Instances of the workspace then run exactly what your CI pipeline built,
instead of building the project inside the Kubernetes cluster.

## How it works

1. Your existing CI pipeline builds the MPS project (generation, compilation, packaging of plugins, ...).
2. At the end of the pipeline, `./gradlew publishModelixWorkspaceArtifact` packages the project folder and its
   dependencies as a ZIP file and uploads it to the workspace-manager.
3. When a workspace instance is started, it runs the unmodified `modelix/mps-vnc-baseimage`.
   Before MPS starts, the artifact and the Modelix plugins are downloaded and installed.
   No docker image is built inside the cluster.

Which artifact an instance uses:

* The artifact selected explicitly for the instance (`artifactId` of the instance), otherwise
* for instances of a git draft, the newest artifact that was built from the draft's base commit, otherwise
* the newest artifact of the workspace.

The workspace must have the build mode `EXTERNAL` (`"buildMode": "EXTERNAL"` in the workspace configuration).

## Usage

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        gradlePluginPortal()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    id("org.modelix.workspaces") version "<version of the modelix Helm chart>"
}

modelixWorkspace {
    serverUrl = "https://modelix.example.com/"
    workspaceId = "6f1d2c3e-..." // shown in the URL of the workspace in the dashboard
    mpsVersion = "2024.1"        // major MPS version the project was built with

    // Opened as a project in MPS. Paths are relative to the folder containing the .mps folder.
    mpsProject("my-project", layout.projectDirectory.dir("mps"))

    // Optional: modules the project depends on. Registered as a global library.
    languages { from(layout.buildDirectory.dir("dependencies")) }

    // Optional: IDEA/MPS plugins (folders or ZIP files) that are installed before MPS starts
    plugins { from(layout.buildDirectory.dir("plugins")) }
}

tasks.named("publishModelixWorkspaceArtifact") {
    dependsOn("build") // whatever task builds your MPS project
}
```

For more control over the content of a project use the `CopySpec` variant:

```kotlin
mpsProject("my-project") {
    from("mps") {
        exclude("**/tmp/**")
    }
}
```

### Tasks

| Task                                        | Description                                                         |
|---------------------------------------------|---------------------------------------------------------------------|
| `generateModelixWorkspaceArtifactManifest`  | Writes `modelix-artifact.json` (MPS version, git commit/branch, label) |
| `packageModelixWorkspaceArtifact`           | Creates `build/modelix/workspace-artifact.zip`                      |
| `publishModelixWorkspaceArtifact`           | Uploads it. The response is written to `build/modelix/published-artifact.json` |

### Authentication

The upload requires the permission `workspaces/workspace/<id>/build-result/write`,
which is granted to the maintainers and owners of a workspace. There are two options:

**Upload token** (simplest): a maintainer of the workspace creates a token that is only valid for uploading artifacts
to this workspace:

```shell
curl -X POST -H "Authorization: Bearer $YOUR_TOKEN" -H "Content-Type: application/json" \
  -d '{"validityDays": 90}' \
  https://modelix.example.com/modelix/workspaces/workspaces/<workspace-id>/artifact-upload-token
```

Store the token as a secret in your CI system and expose it as the environment variable `MODELIX_ACCESS_TOKEN`
(or set `modelixWorkspace.accessToken`).

**OAuth client credentials**: create a client with a service account in the Keycloak realm `modelix`
and grant the permission to the service account user (`service-account-<client-id>`) on the permission management
page of the workspace-manager.

```kotlin
modelixWorkspace {
    oauth {
        clientId = "my-ci-pipeline"
        // clientSecret defaults to the environment variable MODELIX_OAUTH_CLIENT_SECRET
        // tokenUrl defaults to <serverUrl>/realms/modelix/protocol/openid-connect/token
    }
}
```

### Git information

The git commit and branch are detected from the environment variables of common CI systems
(GitHub Actions, GitLab, Jenkins, Azure Pipelines, Bitbucket, TeamCity) or by calling `git`.
They can be overridden with `modelixWorkspace.gitCommit` and `modelixWorkspace.gitBranch`.

The commit is used to find the artifact for instances of git drafts.
Pull request builds usually check out a merge commit that doesn't exist on any branch.
Publish artifacts from builds of branches (e.g. on push) to make them usable for drafts.

## Artifact format

A ZIP file with the following layout. It can also be created without this plugin and uploaded with
`POST /modelix/workspaces/workspaces/<id>/artifacts/upload` (`Content-Type: application/zip`).

```
modelix-artifact.json      optional, {"formatVersion": 1, "mpsVersion": "2024.1", "gitCommit": "...", "gitBranch": "...", "label": "..."}
mps-projects/<name>/...    MPS projects. Copied to /mps-projects/<name> and opened on startup. At least one is required.
mps-languages/...          Copied to /mps-languages, which is registered as a global library
mps-plugins/...            Plugins installed into /mps/plugins before MPS starts
```

The maximum size of an upload is configured in the Helm chart (`workspaces.artifacts.maxUploadSize`, default 2 GiB).
Older artifacts are deleted automatically (`workspaces.artifacts.maxArtifactsPerWorkspace`, default 20),
unless they are used by an instance.
