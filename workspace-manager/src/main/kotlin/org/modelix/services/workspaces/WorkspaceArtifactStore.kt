package org.modelix.services.workspaces

import kotlinx.serialization.json.Json
import org.modelix.services.workspaces.stubs.models.WorkspaceArtifact
import org.modelix.services.workspaces.stubs.models.WorkspaceArtifactManifest
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipException
import java.util.zip.ZipFile

private val LOG = mu.KotlinLogging.logger { }

class InvalidArtifactException(message: String) : IllegalArgumentException(message)
class ArtifactTooLargeException(val maxSizeBytes: Long) : IllegalArgumentException("Artifact exceeds the maximum size of $maxSizeBytes bytes")

/**
 * Stores the results of external (CI) builds of workspaces.
 *
 * Layout on disk:
 *   <rootDir>/<workspaceId>/<artifactId>/content.zip
 *   <rootDir>/<workspaceId>/<artifactId>/artifact.json
 *
 * The artifact.json is written last. Directories without it are incomplete uploads and are ignored.
 */
class WorkspaceArtifactStore(
    val rootDir: File,
    val maxArtifactsPerWorkspace: Int = 20,
) {
    companion object {
        const val MANIFEST_FILE_NAME = "modelix-artifact.json"
        const val PROJECTS_FOLDER = "mps-projects/"
        const val LANGUAGES_FOLDER = "mps-languages/"
        const val PLUGINS_FOLDER = "mps-plugins/"
        private const val CONTENT_FILE_NAME = "content.zip"
        private const val METADATA_FILE_NAME = "artifact.json"
        private val ALLOWED_TOP_LEVEL_FOLDERS = listOf(PROJECTS_FOLDER, LANGUAGES_FOLDER, PLUGINS_FOLDER)
        private val SAFE_ID_PATTERN = Regex("[a-zA-Z0-9_-]{1,100}")
        private val MPS_VERSION_PATTERN = Regex("""20\d\d\.\d""")
        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    private val artifacts = HashMap<String, MutableMap<String, WorkspaceArtifact>>()

    init {
        rootDir.mkdirs()
        loadFromDisk()
        tmpDir().deleteRecursively()
    }

    private fun tmpDir() = rootDir.resolve(".tmp")

    private fun loadFromDisk() {
        for (workspaceDir in rootDir.listFiles().orEmpty()) {
            if (!workspaceDir.isDirectory || !SAFE_ID_PATTERN.matches(workspaceDir.name)) continue
            for (artifactDir in workspaceDir.listFiles().orEmpty()) {
                val metadataFile = artifactDir.resolve(METADATA_FILE_NAME)
                if (!metadataFile.exists()) {
                    LOG.warn { "Deleting incomplete artifact $artifactDir" }
                    artifactDir.deleteRecursively()
                    continue
                }
                try {
                    val artifact = json.decodeFromString(WorkspaceArtifact.serializer(), metadataFile.readText())
                    artifacts.getOrPut(workspaceDir.name) { HashMap() }[artifact.id] = artifact
                } catch (ex: Exception) {
                    LOG.error(ex) { "Failed to load artifact metadata from $metadataFile" }
                }
            }
        }
    }

    /**
     * Stores the ZIP file from [input] as a new artifact.
     *
     * @param protectedArtifactIds provides the IDs of artifacts that must not be deleted when older artifacts are
     *        removed because of [maxArtifactsPerWorkspace], e.g. because they are used by running instances.
     */
    fun store(
        workspaceId: String,
        input: InputStream,
        uploadedBy: String?,
        maxSizeBytes: Long,
        protectedArtifactIds: () -> Set<String> = { emptySet() },
    ): WorkspaceArtifact {
        requireSafeId(workspaceId)
        val artifactId = UUID.randomUUID().toString()
        val tmpFile = tmpDir().also { it.mkdirs() }.resolve("$artifactId.zip")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val size = DigestOutputStream(tmpFile.outputStream().buffered(), digest).use { out ->
                input.copyToLimited(out, maxSizeBytes)
            }
            val manifest = validateAndReadManifest(tmpFile)

            val artifact = WorkspaceArtifact(
                id = artifactId,
                workspaceId = workspaceId,
                createdAt = Instant.now().toString(),
                sizeBytes = size,
                sha256 = digest.digest().joinToString("") { "%02x".format(it) },
                uploadedBy = uploadedBy,
                mpsVersion = manifest?.mpsVersion?.takeIf { it.isNotBlank() },
                gitCommit = manifest?.gitCommit?.takeIf { it.isNotBlank() },
                gitBranch = manifest?.gitBranch?.takeIf { it.isNotBlank() },
                label = manifest?.label?.takeIf { it.isNotBlank() },
            )

            val artifactDir = artifactDir(workspaceId, artifactId)
            artifactDir.mkdirs()
            Files.move(tmpFile.toPath(), artifactDir.resolve(CONTENT_FILE_NAME).toPath(), StandardCopyOption.ATOMIC_MOVE)
            artifactDir.resolve(METADATA_FILE_NAME).writeText(json.encodeToString(WorkspaceArtifact.serializer(), artifact))
            synchronized(artifacts) {
                artifacts.getOrPut(workspaceId) { HashMap() }[artifactId] = artifact
            }
            LOG.info { "Stored artifact $artifactId for workspace $workspaceId (${artifact.sizeBytes} bytes, commit ${artifact.gitCommit})" }

            removeOldArtifacts(workspaceId, protectedArtifactIds() + artifactId)
            return artifact
        } finally {
            tmpFile.delete()
        }
    }

    /**
     * Newest first
     */
    fun list(workspaceId: String): List<WorkspaceArtifact> {
        return synchronized(artifacts) {
            artifacts[workspaceId]?.values.orEmpty().sortedByDescending { Instant.parse(it.createdAt) }
        }
    }

    fun get(workspaceId: String, artifactId: String): WorkspaceArtifact? {
        return synchronized(artifacts) { artifacts[workspaceId]?.get(artifactId) }
    }

    fun getContentFile(workspaceId: String, artifactId: String): File? {
        if (get(workspaceId, artifactId) == null) return null
        return artifactDir(workspaceId, artifactId).resolve(CONTENT_FILE_NAME).takeIf { it.exists() }
    }

    /**
     * The newest artifact that was built from the given git commit or the newest artifact if [gitCommit] is null.
     */
    fun findNewest(workspaceId: String, gitCommit: String? = null): WorkspaceArtifact? {
        return list(workspaceId).firstOrNull { gitCommit == null || it.gitCommit == gitCommit }
    }

    fun delete(workspaceId: String, artifactId: String): Boolean {
        val removed = synchronized(artifacts) { artifacts[workspaceId]?.remove(artifactId) } != null
        if (removed) {
            artifactDir(workspaceId, artifactId).deleteRecursively()
        }
        return removed
    }

    fun deleteAll(workspaceId: String) {
        requireSafeId(workspaceId)
        synchronized(artifacts) { artifacts.remove(workspaceId) }
        rootDir.resolve(workspaceId).deleteRecursively()
    }

    private fun removeOldArtifacts(workspaceId: String, protectedArtifactIds: Set<String>) {
        val toDelete = list(workspaceId)
            .drop(maxArtifactsPerWorkspace)
            .filter { !protectedArtifactIds.contains(it.id) }
        for (artifact in toDelete) {
            LOG.info { "Deleting old artifact ${artifact.id} of workspace $workspaceId" }
            delete(workspaceId, artifact.id)
        }
    }

    private fun artifactDir(workspaceId: String, artifactId: String): File {
        requireSafeId(workspaceId)
        requireSafeId(artifactId)
        return rootDir.resolve(workspaceId).resolve(artifactId)
    }

    private fun requireSafeId(id: String) {
        require(SAFE_ID_PATTERN.matches(id)) { "Invalid ID: $id" }
    }

    private fun validateAndReadManifest(file: File): WorkspaceArtifactManifest? {
        val zip = try {
            ZipFile(file)
        } catch (ex: ZipException) {
            throw InvalidArtifactException("Not a valid ZIP file: ${ex.message}")
        }
        return zip.use {
            var hasProject = false
            for (entry in zip.entries()) {
                val name = entry.name.replace('\\', '/')
                if (name.startsWith("/") || name.split('/').any { it == ".." }) {
                    throw InvalidArtifactException("Invalid path in artifact: ${entry.name}")
                }
                if (name == MANIFEST_FILE_NAME) continue
                if (ALLOWED_TOP_LEVEL_FOLDERS.none { name.startsWith(it) } && ALLOWED_TOP_LEVEL_FOLDERS.none { it == "$name/" }) {
                    throw InvalidArtifactException(
                        "Unexpected entry '${entry.name}'. Only $MANIFEST_FILE_NAME and the folders " +
                            "${ALLOWED_TOP_LEVEL_FOLDERS.joinToString(", ")} are allowed at the top level.",
                    )
                }
                if (name.startsWith(PROJECTS_FOLDER) && name.length > PROJECTS_FOLDER.length) hasProject = true
            }
            if (!hasProject) {
                throw InvalidArtifactException("The artifact doesn't contain any MPS project in the folder $PROJECTS_FOLDER")
            }

            val manifestEntry = zip.getEntry(MANIFEST_FILE_NAME) ?: return@use null
            val manifest = try {
                json.decodeFromString(WorkspaceArtifactManifest.serializer(), zip.getInputStream(manifestEntry).reader().readText())
            } catch (ex: Exception) {
                throw InvalidArtifactException("Invalid $MANIFEST_FILE_NAME: ${ex.message}")
            }
            if (manifest.formatVersion != 1) {
                throw InvalidArtifactException("Unsupported format version ${manifest.formatVersion} in $MANIFEST_FILE_NAME")
            }
            val mpsVersion = manifest.mpsVersion
            if (!mpsVersion.isNullOrBlank() && !MPS_VERSION_PATTERN.matches(mpsVersion)) {
                throw InvalidArtifactException("Invalid major MPS version '$mpsVersion'. Examples for valid values: 2024.1, 2025.1")
            }
            manifest
        }
    }
}

private fun InputStream.copyToLimited(out: java.io.OutputStream, maxSize: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
    var total = 0L
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        total += n
        if (total > maxSize) throw ArtifactTooLargeException(maxSize)
        out.write(buffer, 0, n)
    }
    return total
}
