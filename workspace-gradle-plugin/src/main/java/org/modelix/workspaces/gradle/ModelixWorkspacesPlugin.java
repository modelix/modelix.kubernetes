package org.modelix.workspaces.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.process.ExecOutput;

import java.io.File;

/**
 * Packages the result of a CI build of an MPS project and uploads it to a Modelix workspace.
 * Instances of the workspace then run this artifact instead of building the project inside the cluster.
 *
 * <p>Tasks:
 * <ul>
 *     <li>{@value #PACKAGE_TASK_NAME}: creates the artifact ZIP file</li>
 *     <li>{@value #PUBLISH_TASK_NAME}: uploads it</li>
 * </ul>
 */
public class ModelixWorkspacesPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "modelixWorkspace";
    public static final String MANIFEST_TASK_NAME = "generateModelixWorkspaceArtifactManifest";
    public static final String PACKAGE_TASK_NAME = "packageModelixWorkspaceArtifact";
    public static final String PUBLISH_TASK_NAME = "publishModelixWorkspaceArtifact";
    public static final String TASK_GROUP = "modelix";

    @Override
    public void apply(Project project) {
        ProviderFactory providers = project.getProviders();
        ModelixWorkspaceExtension extension = project.getExtensions().create(EXTENSION_NAME, ModelixWorkspaceExtension.class);

        extension.getAccessToken().convention(nonEmptyEnv(providers, "MODELIX_ACCESS_TOKEN"));
        extension.getOauth().getClientSecret().convention(nonEmptyEnv(providers, "MODELIX_OAUTH_CLIENT_SECRET"));
        extension.getOauth().getTokenUrl().convention(
            extension.getServerUrl().map(url -> url.replaceAll("/+$", "") + "/realms/modelix/protocol/openid-connect/token"));
        extension.getGitCommit().convention(detectGitCommit(project));
        extension.getGitBranch().convention(detectGitBranch(project));
        extension.getLabel().convention(providers.provider(() -> {
            Object version = project.getVersion();
            return Project.DEFAULT_VERSION.equals(version.toString()) ? null : version.toString();
        }));
        extension.getMaxUploadAttempts().convention(3);

        TaskProvider<GenerateWorkspaceArtifactManifest> manifestTask = project.getTasks().register(
            MANIFEST_TASK_NAME, GenerateWorkspaceArtifactManifest.class, task -> {
                task.setGroup(TASK_GROUP);
                task.setDescription("Generates the manifest of the Modelix workspace artifact");
                task.getMpsVersion().set(extension.getMpsVersion());
                task.getGitCommit().set(extension.getGitCommit());
                task.getGitBranch().set(extension.getGitBranch());
                task.getLabel().set(extension.getLabel());
                task.getOutputFile().set(project.getLayout().getBuildDirectory().file(
                    "modelix/manifest/" + GenerateWorkspaceArtifactManifest.FILE_NAME));
            });

        TaskProvider<Zip> packageTask = project.getTasks().register(PACKAGE_TASK_NAME, Zip.class, zip -> {
            zip.setGroup(TASK_GROUP);
            zip.setDescription("Packages the MPS projects, languages and plugins that are used in a Modelix workspace");
            zip.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("modelix"));
            zip.getArchiveFileName().set("workspace-artifact.zip");
            zip.setZip64(true);
            zip.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
            zip.from(manifestTask);
        });
        extension.setPackageTask(packageTask);

        project.getTasks().register(PUBLISH_TASK_NAME, PublishWorkspaceArtifact.class, task -> {
            task.setGroup(TASK_GROUP);
            task.setDescription("Uploads the Modelix workspace artifact. Instances of the workspace will use it.");
            task.getArtifactFile().set(packageTask.flatMap(Zip::getArchiveFile));
            task.getServerUrl().set(extension.getServerUrl());
            task.getWorkspaceId().set(extension.getWorkspaceId());
            task.getAccessToken().set(extension.getAccessToken());
            task.getOauthTokenUrl().set(extension.getOauth().getTokenUrl());
            task.getOauthClientId().set(extension.getOauth().getClientId());
            task.getOauthClientSecret().set(extension.getOauth().getClientSecret());
            task.getMaxAttempts().set(extension.getMaxUploadAttempts());
            task.getResultFile().set(project.getLayout().getBuildDirectory().file("modelix/published-artifact.json"));
        });
    }

    private static Provider<String> nonEmptyEnv(ProviderFactory providers, String name) {
        return providers.environmentVariable(name).map(value -> value.isBlank() ? null : value.trim());
    }

    private static Provider<String> firstEnv(ProviderFactory providers, String... names) {
        Provider<String> result = nonEmptyEnv(providers, names[0]);
        for (int i = 1; i < names.length; i++) {
            result = result.orElse(nonEmptyEnv(providers, names[i]));
        }
        return result;
    }

    private static Provider<String> detectGitCommit(Project project) {
        ProviderFactory providers = project.getProviders();
        return firstEnv(providers,
            "GITHUB_SHA", // GitHub Actions
            "CI_COMMIT_SHA", // GitLab
            "GIT_COMMIT", // Jenkins
            "BUILD_SOURCEVERSION", // Azure Pipelines
            "BITBUCKET_COMMIT", // Bitbucket Pipelines
            "BUILD_VCS_NUMBER" // TeamCity
        ).orElse(git(project, "rev-parse", "HEAD"));
    }

    private static Provider<String> detectGitBranch(Project project) {
        ProviderFactory providers = project.getProviders();
        return firstEnv(providers,
            "GITHUB_HEAD_REF", // GitHub Actions (pull requests)
            "GITHUB_REF_NAME", // GitHub Actions
            "CI_COMMIT_REF_NAME", // GitLab
            "BRANCH_NAME", // Jenkins multibranch
            "GIT_BRANCH", // Jenkins
            "BUILD_SOURCEBRANCHNAME", // Azure Pipelines
            "BITBUCKET_BRANCH" // Bitbucket Pipelines
        ).map(branch -> branch.startsWith("origin/") ? branch.substring("origin/".length()) : branch)
            .orElse(git(project, "rev-parse", "--abbrev-ref", "HEAD").map(branch -> "HEAD".equals(branch) ? null : branch));
    }

    /**
     * Output of a git command, or no value if git isn't available or the project isn't inside a git repository.
     */
    private static Provider<String> git(Project project, String... args) {
        ProviderFactory providers = project.getProviders();
        File projectDir = project.getProjectDir();
        return providers.provider(() -> {
            try {
                ExecOutput output = providers.exec(spec -> {
                    spec.setWorkingDir(projectDir);
                    spec.commandLine((Object[]) prepend("git", args));
                    spec.setIgnoreExitValue(true);
                });
                if (output.getResult().get().getExitValue() != 0) return null;
                String value = output.getStandardOutput().getAsText().get().trim();
                return value.isEmpty() ? null : value;
            } catch (Exception ex) {
                // git not installed
                return null;
            }
        });
    }

    private static String[] prepend(String first, String[] rest) {
        String[] result = new String[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }
}
