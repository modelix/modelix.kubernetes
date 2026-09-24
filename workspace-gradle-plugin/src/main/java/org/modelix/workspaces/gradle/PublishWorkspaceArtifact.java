package org.modelix.workspaces.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Uploads the artifact to the workspace-manager of a Modelix instance.
 * The response (JSON description of the stored artifact) is written to {@link #getResultFile()}.
 */
@DisableCachingByDefault(because = "Uploading has side effects")
public abstract class PublishWorkspaceArtifact extends DefaultTask {

    public PublishWorkspaceArtifact() {
        getOutputs().upToDateWhen(task -> false);
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getArtifactFile();

    @Input
    public abstract Property<String> getServerUrl();

    @Input
    public abstract Property<String> getWorkspaceId();

    @Internal
    public abstract Property<String> getAccessToken();

    @Internal
    public abstract Property<String> getOauthTokenUrl();

    @Internal
    public abstract Property<String> getOauthClientId();

    @Internal
    public abstract Property<String> getOauthClientSecret();

    @Internal
    public abstract Property<Integer> getMaxAttempts();

    @OutputFile
    public abstract RegularFileProperty getResultFile();

    @TaskAction
    public void publish() throws IOException, InterruptedException {
        String serverUrl = getServerUrl().get().replaceAll("/+$", "");
        String workspaceId = getWorkspaceId().get();
        Path artifact = getArtifactFile().get().getAsFile().toPath();
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        String token = resolveAccessToken(client);
        URI uploadUri = URI.create(serverUrl + "/modelix/workspaces/workspaces/"
            + URLEncoder.encode(workspaceId, StandardCharsets.UTF_8).replace("+", "%20")
            + "/artifacts/upload");
        HttpRequest request = HttpRequest.newBuilder(uploadUri)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/zip")
            .timeout(Duration.ofMinutes(30))
            .POST(HttpRequest.BodyPublishers.ofFile(artifact))
            .build();

        getLogger().lifecycle("Uploading {} ({} MiB) to {}", artifact.getFileName(), Files.size(artifact) / 1024 / 1024, uploadUri);
        HttpResponse<String> response = sendWithRetries(client, request);
        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new GradleException(describeFailure(response));
        }

        String body = response.body();
        Files.write(getResultFile().get().getAsFile().toPath(), body.getBytes(StandardCharsets.UTF_8));
        getLogger().lifecycle("Uploaded artifact {} for workspace {}", Json.readStringProperty(body, "id"), workspaceId);
    }

    private HttpResponse<String> sendWithRetries(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        int maxAttempts = Math.max(1, getMaxAttempts().getOrElse(3));
        for (int attempt = 1; ; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                boolean retryable = response.statusCode() == 502 || response.statusCode() == 503 || response.statusCode() == 504;
                if (!retryable || attempt >= maxAttempts) return response;
                getLogger().warn("Upload failed with status {}. Retrying ({}/{})", response.statusCode(), attempt, maxAttempts);
            } catch (IOException ex) {
                if (attempt >= maxAttempts) throw ex;
                getLogger().warn("Upload failed: {}. Retrying ({}/{})", ex.getMessage(), attempt, maxAttempts);
            }
            Thread.sleep(5000L * attempt);
        }
    }

    private String resolveAccessToken(HttpClient client) throws IOException, InterruptedException {
        String token = getAccessToken().getOrNull();
        if (token != null && !token.isBlank()) return token.trim();

        String clientId = getOauthClientId().getOrNull();
        if (clientId == null) {
            throw new GradleException("No credentials for uploading the workspace artifact. "
                + "Set modelixWorkspace.accessToken (or the environment variable MODELIX_ACCESS_TOKEN) "
                + "or configure modelixWorkspace.oauth { clientId = ...; clientSecret = ... }");
        }
        String clientSecret = getOauthClientSecret().getOrNull();
        if (clientSecret == null) {
            throw new GradleException("modelixWorkspace.oauth.clientSecret (or the environment variable MODELIX_OAUTH_CLIENT_SECRET) is not set");
        }

        String form = "grant_type=client_credentials"
            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(getOauthTokenUrl().get()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofMinutes(1))
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new GradleException("Requesting an access token failed. " + describeFailure(response));
        }
        String accessToken = Json.readStringProperty(response.body(), "access_token");
        if (accessToken == null) {
            throw new GradleException("Token response from " + request.uri() + " doesn't contain an access_token");
        }
        return accessToken;
    }

    private static String describeFailure(HttpResponse<String> response) {
        String hint = "";
        if (response.statusCode() == 401) hint = " The access token is missing, invalid or expired.";
        if (response.statusCode() == 403) hint = " The access token doesn't grant the permission to upload artifacts for this workspace.";
        if (response.statusCode() == 413) hint = " The artifact is too large. The limit is configured in the Helm chart (workspaces.artifacts.maxUploadSize).";
        return response.request().method() + " " + response.request().uri() + " failed with status " + response.statusCode()
            + "." + hint + "\n" + response.body();
    }
}
