package org.modelix.workspace.manager

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.modelix.authorization.ModelixAuthorization
import org.modelix.authorization.ModelixJWTUtil
import org.modelix.authorization.permissions.PermissionParts
import org.modelix.services.workspaces.WorkspaceArtifactStore
import org.modelix.services.workspaces.WorkspaceArtifactsController
import org.modelix.services.workspaces.stubs.models.ArtifactUploadToken
import org.modelix.services.workspaces.stubs.models.ArtifactUploadTokenRequest
import org.modelix.services.workspaces.stubs.models.WorkspaceArtifact
import org.modelix.services.workspaces.stubs.models.WorkspaceArtifactList
import org.modelix.workspaces.WorkspacesPermissionSchema
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class WorkspaceArtifactsControllerTest {
    private val hmacKey = "unit-test-key-".repeat(10)
    private val workspaceId = "ws1"
    private val workspace = WorkspacesPermissionSchema.workspaces.workspace(workspaceId)
    private val rootDir = Files.createTempDirectory("artifact-controller-test").toFile()
    private val jwtUtil = ModelixJWTUtil().also { it.setHmac512Key(hmacKey) }

    @AfterTest
    fun cleanup() {
        rootDir.deleteRecursively()
    }

    private fun token(vararg permissions: PermissionParts) =
        jwtUtil.createAccessToken("unit-test@example.com", permissions.map { it.fullId })

    private val validZip: ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("modelix-artifact.json"))
            zip.write("""{"formatVersion": 1, "gitCommit": "abc"}""".toByteArray())
            zip.putNextEntry(ZipEntry("mps-projects/p/.mps/modules.xml"))
            zip.write("<project/>".toByteArray())
        }
    }.toByteArray()

    private fun runTest(maxUploadSize: Long = 1024 * 1024, body: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            install(ModelixAuthorization) {
                permissionSchema = WorkspacesPermissionSchema.SCHEMA
                hmac512Key = hmacKey
                permissionChecksEnabled = true
                installStatusPages = true
            }
            install(ServerContentNegotiation) { json() }
            routing {
                WorkspaceArtifactsController(
                    artifactStore = WorkspaceArtifactStore(rootDir),
                    jwtUtil = jwtUtil,
                    workspaceExists = { it == workspaceId },
                    artifactIdsInUse = { emptySet() },
                    maxUploadSizeBytes = maxUploadSize,
                ).install(this)
            }
        }
        body()
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    private suspend fun ApplicationTestBuilder.upload(token: String, content: ByteArray = validZip, ws: String = workspaceId) =
        client.post("/modelix/workspaces/workspaces/$ws/artifacts/upload") {
            bearerAuth(token)
            contentType(ContentType.Application.Zip)
            setBody(content)
        }

    @Test
    fun `upload, list, download and delete`() = runTest {
        val writeToken = token(workspace.buildResult.write)
        val uploadResponse = upload(writeToken)
        assertEquals(HttpStatusCode.Created, uploadResponse.status)
        val artifact = jsonClient().post("/modelix/workspaces/workspaces/$workspaceId/artifacts/upload") {
            bearerAuth(writeToken)
            contentType(ContentType.Application.Zip)
            setBody(validZip)
        }.body<WorkspaceArtifact>()
        assertEquals("abc", artifact.gitCommit)

        val readToken = token(workspace.viewer)
        val list = jsonClient().get("/modelix/workspaces/workspaces/$workspaceId/artifacts/") { bearerAuth(readToken) }
            .body<WorkspaceArtifactList>()
        assertEquals(2, list.artifacts.size)
        assertEquals(artifact.id, list.artifacts.first().id)

        val download = client.get("/modelix/workspaces/workspaces/$workspaceId/artifacts/${artifact.id}/content.zip") { bearerAuth(readToken) }
        assertEquals(HttpStatusCode.OK, download.status)
        assertContentEquals(validZip, download.readRawBytes())

        assertEquals(
            HttpStatusCode.Forbidden,
            client.delete("/modelix/workspaces/workspaces/$workspaceId/artifacts/${artifact.id}") { bearerAuth(readToken) }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.delete("/modelix/workspaces/workspaces/$workspaceId/artifacts/${artifact.id}") { bearerAuth(writeToken) }.status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/modelix/workspaces/workspaces/$workspaceId/artifacts/${artifact.id}") { bearerAuth(readToken) }.status,
        )
    }

    @Test
    fun `upload requires write permission`() = runTest {
        assertEquals(HttpStatusCode.Forbidden, upload(token(workspace.viewer)).status)
        assertEquals(HttpStatusCode.Forbidden, upload(token(WorkspacesPermissionSchema.workspaces.workspace("other").buildResult.write)).status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/modelix/workspaces/workspaces/$workspaceId/artifacts/upload") { setBody(validZip) }.status)
    }

    @Test
    fun `invalid artifact is rejected`() = runTest {
        assertEquals(HttpStatusCode.BadRequest, upload(token(workspace.buildResult.write), "no zip".toByteArray()).status)
    }

    @Test
    fun `too large artifact is rejected`() = runTest(maxUploadSize = 10) {
        assertEquals(HttpStatusCode.PayloadTooLarge, upload(token(workspace.buildResult.write)).status)
    }

    @Test
    fun `upload to unknown workspace fails`() = runTest {
        val ws = WorkspacesPermissionSchema.workspaces.workspace("unknown")
        assertEquals(HttpStatusCode.NotFound, upload(token(ws.buildResult.write), ws = "unknown").status)
    }

    @Test
    fun `upload token can upload but not create new tokens`() = runTest {
        val client = jsonClient()
        val tokenUrl = "/modelix/workspaces/workspaces/$workspaceId/artifact-upload-token"

        val uploadToken = client.post(tokenUrl) {
            bearerAuth(token(workspace.maintainer))
            contentType(ContentType.Application.Json)
            setBody(ArtifactUploadTokenRequest(validityDays = 10))
        }.body<ArtifactUploadToken>()

        assertEquals(HttpStatusCode.Created, upload(uploadToken.token).status)
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post(tokenUrl) {
                bearerAuth(uploadToken.token)
                contentType(ContentType.Application.Json)
                setBody(ArtifactUploadTokenRequest())
            }.status,
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.post(tokenUrl) {
                bearerAuth(token(workspace.contributor))
                contentType(ContentType.Application.Json)
                setBody(ArtifactUploadTokenRequest())
            }.status,
        )
    }
}
