package org.modelix.workspaces.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Writes the modelix-artifact.json that describes the content of the artifact.
 */
@CacheableTask
public abstract class GenerateWorkspaceArtifactManifest extends DefaultTask {
    public static final String FILE_NAME = "modelix-artifact.json";
    private static final Pattern MPS_VERSION_PATTERN = Pattern.compile("20\\d\\d\\.\\d");

    @Input
    @Optional
    public abstract Property<String> getMpsVersion();

    @Input
    @Optional
    public abstract Property<String> getGitCommit();

    @Input
    @Optional
    public abstract Property<String> getGitBranch();

    @Input
    @Optional
    public abstract Property<String> getLabel();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void generate() throws IOException {
        String mpsVersion = getMpsVersion().getOrNull();
        if (mpsVersion != null && !MPS_VERSION_PATTERN.matcher(mpsVersion).matches()) {
            throw new IllegalArgumentException(
                "Invalid MPS version '" + mpsVersion + "'. Specify the major version, e.g. 2024.1 or 2025.1");
        }

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("formatVersion", 1);
        manifest.put("mpsVersion", mpsVersion);
        manifest.put("gitCommit", getGitCommit().getOrNull());
        manifest.put("gitBranch", getGitBranch().getOrNull());
        manifest.put("label", getLabel().getOrNull());
        Files.write(getOutputFile().get().getAsFile().toPath(), Json.toJson(manifest).getBytes(StandardCharsets.UTF_8));
    }
}
