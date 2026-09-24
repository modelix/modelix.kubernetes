package org.modelix.workspaces.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.CopySpec;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import javax.inject.Inject;

/**
 * Configuration of the {@code modelixWorkspace} extension.
 *
 * <pre>
 * modelixWorkspace {
 *     serverUrl = "https://modelix.example.com/"
 *     workspaceId = "6f1d..."
 *     mpsVersion = "2024.1"
 *     accessToken = providers.environmentVariable("MODELIX_ACCESS_TOKEN")
 *
 *     mpsProject("my-project", layout.projectDirectory.dir("mps"))
 *     languages { from(layout.buildDirectory.dir("dependencies")) }
 *     plugins { from(layout.buildDirectory.dir("plugins")) }
 * }
 * </pre>
 */
public abstract class ModelixWorkspaceExtension {
    public static final String PROJECTS_FOLDER = "mps-projects";
    public static final String LANGUAGES_FOLDER = "mps-languages";
    public static final String PLUGINS_FOLDER = "mps-plugins";

    private final OAuthClientCredentials oauth;
    private TaskProvider<Zip> packageTask;

    @Inject
    public ModelixWorkspaceExtension(ObjectFactory objects) {
        oauth = objects.newInstance(OAuthClientCredentials.class);
    }

    /**
     * Base URL of the Modelix instance, e.g. https://modelix.example.com/
     */
    public abstract Property<String> getServerUrl();

    /**
     * ID of the workspace the artifact is uploaded to. It is shown in the URL of the workspace in the dashboard.
     */
    public abstract Property<String> getWorkspaceId();

    /**
     * Major MPS version (e.g. 2024.1) the project was built with. The workspace instance is started with the same version.
     * If not specified, the MPS version configured in the workspace is used.
     */
    public abstract Property<String> getMpsVersion();

    /**
     * Token with the permission workspaces/workspace/&lt;id&gt;/build-result/write.
     * Can be created in the dashboard or through the REST API (POST /modelix/workspaces/workspaces/&lt;id&gt;/artifact-upload-token).
     * Defaults to the environment variable MODELIX_ACCESS_TOKEN.
     * Alternatively, configure {@link #oauth(Action)} to request a token with client credentials.
     */
    public abstract Property<String> getAccessToken();

    /**
     * Git commit the artifact was built from. Instances of a draft use the artifact of the draft's base commit.
     * Detected from common CI environment variables or the git repository.
     */
    public abstract Property<String> getGitCommit();

    /**
     * Git branch the artifact was built from. Detected from common CI environment variables or the git repository.
     */
    public abstract Property<String> getGitBranch();

    /**
     * Free text, e.g. a build number. Defaults to the version of the Gradle project.
     */
    public abstract Property<String> getLabel();

    /**
     * Number of attempts for uploading the artifact in case of network errors. Defaults to 3.
     */
    public abstract Property<Integer> getMaxUploadAttempts();

    /**
     * Credentials for requesting an access token using the OAuth client credentials flow
     * (e.g. a Keycloak client with a service account).
     */
    public OAuthClientCredentials getOauth() {
        return oauth;
    }

    public void oauth(Action<? super OAuthClientCredentials> action) {
        action.execute(oauth);
    }

    void setPackageTask(TaskProvider<Zip> packageTask) {
        this.packageTask = packageTask;
    }

    /**
     * Adds an MPS project to the artifact. The project is opened when the workspace instance starts.
     *
     * @param name name of the project folder inside the workspace instance
     * @param content files of the project. Paths are relative to the project folder.
     */
    public void mpsProject(String name, Action<? super CopySpec> content) {
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals("..") || name.equals(".")) {
            throw new IllegalArgumentException("Invalid project name: " + name);
        }
        packageTask.configure(zip -> zip.into(PROJECTS_FOLDER + "/" + name, content));
    }

    /**
     * Adds an MPS project to the artifact. The project is opened when the workspace instance starts.
     *
     * @param name name of the project folder inside the workspace instance
     * @param projectDir the folder containing the .mps folder. Resolved using {@code project.file(...)}.
     */
    public void mpsProject(String name, Object projectDir) {
        mpsProject(name, spec -> {
            spec.from(projectDir);
            spec.exclude(".gradle/**");
        });
    }

    /**
     * Additional modules (e.g. dependencies of the project). They are registered as a global library in MPS.
     */
    public void languages(Action<? super CopySpec> content) {
        packageTask.configure(zip -> zip.into(LANGUAGES_FOLDER, content));
    }

    /**
     * IDEA/MPS plugins that are installed before MPS starts. Each plugin can be a folder or a ZIP file.
     */
    public void plugins(Action<? super CopySpec> content) {
        packageTask.configure(zip -> zip.into(PLUGINS_FOLDER, content));
    }
}
