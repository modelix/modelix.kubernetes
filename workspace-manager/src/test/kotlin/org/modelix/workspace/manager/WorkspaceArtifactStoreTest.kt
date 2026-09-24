package org.modelix.workspace.manager

import org.modelix.services.workspaces.ArtifactTooLargeException
import org.modelix.services.workspaces.InvalidArtifactException
import org.modelix.services.workspaces.WorkspaceArtifactStore
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceArtifactStoreTest {
    private val rootDir = Files.createTempDirectory("artifact-store-test").toFile()
    private val workspaceId = "0a1b2c3d-0000-1111-2222-333344445555"

    @AfterTest
    fun cleanup() {
        rootDir.deleteRecursively()
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun validZip(commit: String = "abc123", mpsVersion: String = "2024.1") = zip(
        "modelix-artifact.json" to """{"formatVersion": 1, "mpsVersion": "$mpsVersion", "gitCommit": "$commit", "gitBranch": "main", "label": "build 7", "unknownProperty": 1}""",
        "mps-projects/my-project/.mps/modules.xml" to "<project/>",
        "mps-languages/lang.jar" to "jar",
    )

    private fun WorkspaceArtifactStore.store(content: ByteArray, maxSize: Long = 1024 * 1024, protected: Set<String> = emptySet()) =
        store(workspaceId, content.inputStream(), "user@example.com", maxSize) { protected }

    @Test
    fun `stores artifact and reads manifest`() {
        val store = WorkspaceArtifactStore(rootDir)
        val content = validZip()
        val artifact = store.store(content)

        assertEquals(workspaceId, artifact.workspaceId)
        assertEquals("2024.1", artifact.mpsVersion)
        assertEquals("abc123", artifact.gitCommit)
        assertEquals("main", artifact.gitBranch)
        assertEquals("build 7", artifact.label)
        assertEquals("user@example.com", artifact.uploadedBy)
        assertEquals(content.size.toLong(), artifact.sizeBytes)
        assertEquals(64, artifact.sha256.length)
        assertContentEquals(content, assertNotNull(store.getContentFile(workspaceId, artifact.id)).readBytes())
        assertEquals(listOf(artifact), store.list(workspaceId))
    }

    @Test
    fun `manifest is optional`() {
        val store = WorkspaceArtifactStore(rootDir)
        val artifact = store.store(zip("mps-projects/p/.mps/modules.xml" to ""))
        assertNull(artifact.mpsVersion)
        assertNull(artifact.gitCommit)
    }

    @Test
    fun `artifacts are loaded from disk after restart`() {
        val artifact = WorkspaceArtifactStore(rootDir).store(validZip())
        val reloaded = WorkspaceArtifactStore(rootDir)
        assertEquals(artifact, reloaded.get(workspaceId, artifact.id))
        assertNotNull(reloaded.getContentFile(workspaceId, artifact.id))
    }

    @Test
    fun `finds newest artifact for commit`() {
        val store = WorkspaceArtifactStore(rootDir)
        val a1 = store.store(validZip(commit = "c1"))
        Thread.sleep(5)
        val a2 = store.store(validZip(commit = "c2"))
        Thread.sleep(5)
        val a3 = store.store(validZip(commit = "c1"))

        assertEquals(a3.id, store.findNewest(workspaceId, "c1")?.id)
        assertEquals(a2.id, store.findNewest(workspaceId, "c2")?.id)
        assertEquals(a3.id, store.findNewest(workspaceId)?.id)
        assertNull(store.findNewest(workspaceId, "c3"))
        assertEquals(listOf(a3.id, a2.id, a1.id), store.list(workspaceId).map { it.id })
    }

    @Test
    fun `old artifacts are deleted unless protected`() {
        val store = WorkspaceArtifactStore(rootDir, maxArtifactsPerWorkspace = 2)
        val a1 = store.store(validZip())
        Thread.sleep(5)
        val a2 = store.store(validZip())
        Thread.sleep(5)
        val a3 = store.store(validZip(), protected = setOf(a1.id))
        assertEquals(listOf(a3.id, a2.id, a1.id), store.list(workspaceId).map { it.id })

        Thread.sleep(5)
        val a4 = store.store(validZip())
        assertEquals(listOf(a4.id, a3.id), store.list(workspaceId).map { it.id })
        assertNull(store.getContentFile(workspaceId, a1.id))
        assertTrue(!rootDir.resolve(workspaceId).resolve(a1.id).exists())
    }

    @Test
    fun `rejects artifact without project`() {
        val store = WorkspaceArtifactStore(rootDir)
        assertFailsWith<InvalidArtifactException> {
            store.store(zip("mps-languages/lang.jar" to "jar"))
        }
        assertEquals(emptyList(), store.list(workspaceId))
        assertEquals(emptyList(), rootDir.resolve(".tmp").listFiles().orEmpty().toList())
    }

    @Test
    fun `rejects unexpected top level entries`() {
        val store = WorkspaceArtifactStore(rootDir)
        assertFailsWith<InvalidArtifactException> {
            store.store(zip("mps-projects/p/.mps/modules.xml" to "", "build.gradle.kts" to ""))
        }
    }

    @Test
    fun `rejects path traversal`() {
        val store = WorkspaceArtifactStore(rootDir)
        assertFailsWith<InvalidArtifactException> {
            store.store(zip("mps-projects/p/.mps/modules.xml" to "", "mps-projects/../../evil.sh" to ""))
        }
    }

    @Test
    fun `rejects invalid mps version`() {
        val store = WorkspaceArtifactStore(rootDir)
        assertFailsWith<InvalidArtifactException> {
            store.store(validZip(mpsVersion = "2024.1.4"))
        }
    }

    @Test
    fun `rejects non zip content`() {
        val store = WorkspaceArtifactStore(rootDir)
        assertFailsWith<InvalidArtifactException> {
            store.store("not a zip".toByteArray())
        }
    }

    @Test
    fun `rejects too large artifact`() {
        val store = WorkspaceArtifactStore(rootDir)
        val content = validZip()
        assertFailsWith<ArtifactTooLargeException> {
            store.store(content, maxSize = content.size - 1L)
        }
        assertEquals(emptyList(), store.list(workspaceId))
    }

    @Test
    fun `rejects unsafe workspace id`() {
        val store = WorkspaceArtifactStore(rootDir)
        assertFailsWith<IllegalArgumentException> {
            store.store("../x", validZip().inputStream(), null, 1024 * 1024)
        }
    }

    @Test
    fun `delete all removes workspace folder`() {
        val store = WorkspaceArtifactStore(rootDir)
        store.store(validZip())
        store.deleteAll(workspaceId)
        assertEquals(emptyList(), store.list(workspaceId))
        assertTrue(!rootDir.resolve(workspaceId).exists())
    }
}
