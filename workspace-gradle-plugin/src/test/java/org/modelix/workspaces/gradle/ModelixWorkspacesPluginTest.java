package org.modelix.workspaces.gradle;

import com.sun.net.httpserver.HttpServer;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelixWorkspacesPluginTest {

    @TempDir
    Path projectDir;

    private HttpServer server;
    private final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile int uploadStatus = 201;

    record RecordedRequest(String method, String path, Map<String, String> headers, byte[] body) {
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(), v.get(0)));
            String path = exchange.getRequestURI().getPath();
            requests.add(new RecordedRequest(exchange.getRequestMethod(), path, headers, body));

            int status;
            String response;
            if (path.equals("/realms/modelix/protocol/openid-connect/token")) {
                status = 200;
                response = "{\"access_token\":\"token-from-oauth\",\"expires_in\":300}";
            } else if (path.endsWith("/artifacts/upload")) {
                status = uploadStatus;
                response = status == 201 ? "{\"id\":\"artifact-1\",\"workspaceId\":\"ws1\"}" : "no permission";
            } else {
                status = 404;
                response = "not found";
            }
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String serverUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private void writeProject(String extensionConfig) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'test'\n");
        Files.writeString(projectDir.resolve("build.gradle"), String.join("\n",
            "plugins { id 'org.modelix.workspaces' }",
            "version = '1.2.3'",
            "modelixWorkspace {",
            "    serverUrl = '" + serverUrl() + "'",
            "    workspaceId = 'ws1'",
            "    mpsVersion = '2024.1'",
            "    mpsProject('my-project', file('mps'))",
            "    languages { from('deps') }",
            "    plugins { from('plugins') }",
            extensionConfig,
            "}",
            ""));
        write("mps/.mps/modules.xml", "<project/>");
        write("mps/solutions/a/a.msd", "<solution/>");
        write("mps/.gradle/ignored.txt", "ignored");
        write("deps/lib.jar", "jar");
        write("plugins/my-plugin/lib/my-plugin.jar", "jar");
    }

    private void write(String path, String content) throws IOException {
        Path file = projectDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private GradleRunner runner(Map<String, String> env, String... args) {
        List<String> arguments = new ArrayList<>(List.of(args));
        arguments.add("--stacktrace");
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withEnvironment(env)
            .withArguments(arguments);
    }

    private static Map<String, String> ciEnv(String... additional) {
        Map<String, String> env = new HashMap<>();
        env.put("GITHUB_SHA", "0123456789abcdef");
        env.put("GITHUB_HEAD_REF", "");
        env.put("GITHUB_REF_NAME", "main");
        for (int i = 0; i < additional.length; i += 2) {
            env.put(additional[i], additional[i + 1]);
        }
        return env;
    }

    private RecordedRequest uploadRequest() {
        return requests.stream().filter(it -> it.path().endsWith("/artifacts/upload")).findFirst().orElseThrow();
    }

    private static Map<String, String> unzip(byte[] zip) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                entries.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    @Test
    void uploadsArtifactWithAccessToken() throws IOException {
        writeProject("");
        BuildResult result = runner(ciEnv("MODELIX_ACCESS_TOKEN", "my-token"), "publishModelixWorkspaceArtifact").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModelixWorkspaceArtifact").getOutcome());
        assertTrue(result.getOutput().contains("Uploaded artifact artifact-1 for workspace ws1"), result.getOutput());

        RecordedRequest upload = uploadRequest();
        assertEquals("POST", upload.method());
        assertEquals("/modelix/workspaces/workspaces/ws1/artifacts/upload", upload.path());
        assertEquals("Bearer my-token", upload.headers().get("authorization"));
        assertEquals("application/zip", upload.headers().get("content-type"));

        Map<String, String> entries = unzip(upload.body());
        assertEquals(new TreeSet<>(List.of(
            "modelix-artifact.json",
            "mps-projects/my-project/.mps/modules.xml",
            "mps-projects/my-project/solutions/a/a.msd",
            "mps-languages/lib.jar",
            "mps-plugins/my-plugin/lib/my-plugin.jar"
        )), new TreeSet<>(entries.keySet()));

        String manifest = entries.get("modelix-artifact.json");
        assertEquals("2024.1", Json.readStringProperty(manifest, "mpsVersion"));
        assertEquals("0123456789abcdef", Json.readStringProperty(manifest, "gitCommit"));
        assertEquals("main", Json.readStringProperty(manifest, "gitBranch"));
        assertEquals("1.2.3", Json.readStringProperty(manifest, "label"));
        assertTrue(manifest.contains("\"formatVersion\": 1"), manifest);

        Path resultFile = projectDir.resolve("build/modelix/published-artifact.json");
        assertTrue(Files.readString(resultFile).contains("artifact-1"));
    }

    @Test
    void requestsTokenWithClientCredentials() throws IOException {
        writeProject("    oauth { clientId = 'ci-client' }");
        runner(ciEnv("MODELIX_OAUTH_CLIENT_SECRET", "s3cr3t&x"), "publishModelixWorkspaceArtifact").build();

        RecordedRequest tokenRequest = requests.get(0);
        assertEquals("/realms/modelix/protocol/openid-connect/token", tokenRequest.path());
        assertEquals("grant_type=client_credentials&client_id=ci-client&client_secret=s3cr3t%26x",
            new String(tokenRequest.body(), StandardCharsets.UTF_8));
        assertEquals("Bearer token-from-oauth", uploadRequest().headers().get("authorization"));
    }

    @Test
    void failsWithHelpfulMessageWithoutPermission() throws IOException {
        uploadStatus = 403;
        writeProject("");
        BuildResult result = runner(ciEnv("MODELIX_ACCESS_TOKEN", "my-token"), "publishModelixWorkspaceArtifact").buildAndFail();
        assertTrue(result.getOutput().contains("failed with status 403"), result.getOutput());
        assertTrue(result.getOutput().contains("doesn't grant the permission"), result.getOutput());
    }

    @Test
    void failsWithoutCredentials() throws IOException {
        writeProject("");
        BuildResult result = runner(ciEnv(), "publishModelixWorkspaceArtifact").buildAndFail();
        assertTrue(result.getOutput().contains("No credentials for uploading the workspace artifact"), result.getOutput());
        assertTrue(requests.isEmpty());
    }

    @Test
    void rejectsInvalidMpsVersion() throws IOException {
        writeProject("    mpsVersion = '2024.1.4'");
        BuildResult result = runner(ciEnv(), "packageModelixWorkspaceArtifact").buildAndFail();
        assertTrue(result.getOutput().contains("Invalid MPS version '2024.1.4'"), result.getOutput());
    }

    @Test
    void readsGitInformationFromRepository() throws IOException, InterruptedException {
        writeProject("");
        exec("git", "init", "-q", "-b", "feature-x");
        exec("git", "-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-q", "--allow-empty", "-m", "initial");
        String commit = new String(new ProcessBuilder("git", "rev-parse", "HEAD").directory(projectDir.toFile()).start()
            .getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        Map<String, String> env = new HashMap<>(System.getenv());
        for (String name : List.of("GITHUB_SHA", "GITHUB_HEAD_REF", "GITHUB_REF_NAME", "CI_COMMIT_SHA", "CI_COMMIT_REF_NAME",
            "GIT_COMMIT", "GIT_BRANCH", "BRANCH_NAME")) {
            env.remove(name);
        }
        runner(env, "packageModelixWorkspaceArtifact").build();

        String manifest = Files.readString(projectDir.resolve("build/modelix/manifest/modelix-artifact.json"));
        assertEquals(commit, Json.readStringProperty(manifest, "gitCommit"));
        assertEquals("feature-x", Json.readStringProperty(manifest, "gitBranch"));
    }

    @Test
    void compatibleWithConfigurationCache() throws IOException {
        writeProject("");
        Map<String, String> env = ciEnv("MODELIX_ACCESS_TOKEN", "my-token");
        BuildResult first = runner(env, "publishModelixWorkspaceArtifact", "--configuration-cache").build();
        assertTrue(first.getOutput().contains("Configuration cache entry stored"), first.getOutput());
        BuildResult second = runner(env, "publishModelixWorkspaceArtifact", "--configuration-cache").build();
        assertTrue(second.getOutput().contains("Configuration cache entry reused"), second.getOutput());
        assertEquals(TaskOutcome.SUCCESS, second.task(":publishModelixWorkspaceArtifact").getOutcome());
        assertEquals(2, requests.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.6.4", "8.14.3"})
    void compatibleWithOlderGradleVersions(String gradleVersion) throws IOException {
        writeProject("");
        BuildResult result = runner(ciEnv("MODELIX_ACCESS_TOKEN", "my-token"), "publishModelixWorkspaceArtifact")
            .withGradleVersion(gradleVersion)
            .build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModelixWorkspaceArtifact").getOutcome());
        assertNotNull(unzip(uploadRequest().body()).get("mps-projects/my-project/.mps/modules.xml"));
    }

    private void exec(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).directory(projectDir.toFile()).inheritIO().start();
        assertEquals(0, process.waitFor());
    }
}
