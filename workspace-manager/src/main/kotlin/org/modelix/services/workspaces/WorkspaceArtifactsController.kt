package org.modelix.services.workspaces

import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveStream
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.modelix.authorization.ModelixJWTUtil
import org.modelix.authorization.checkPermission
import org.modelix.authorization.getUserName
import org.modelix.services.workspaces.stubs.controllers.ModelixWorkspacesWorkspacesArtifactUploadTokenController
import org.modelix.services.workspaces.stubs.controllers.ModelixWorkspacesWorkspacesArtifactUploadTokenController.Companion.modelixWorkspacesWorkspacesArtifactUploadTokenRoutes
import org.modelix.services.workspaces.stubs.controllers.ModelixWorkspacesWorkspacesArtifactsContentZipController
import org.modelix.services.workspaces.stubs.controllers.ModelixWorkspacesWorkspacesArtifactsContentZipController.Companion.modelixWorkspacesWorkspacesArtifactsContentZipRoutes
import org.modelix.services.workspaces.stubs.controllers.ModelixWorkspacesWorkspacesArtifactsController
import org.modelix.services.workspaces.stubs.controllers.ModelixWorkspacesWorkspacesArtifactsController.Companion.modelixWorkspacesWorkspacesArtifactsRoutes
import org.modelix.services.workspaces.stubs.controllers.TypedApplicationCall
import org.modelix.services.workspaces.stubs.models.ArtifactUploadToken
import org.modelix.services.workspaces.stubs.models.ArtifactUploadTokenRequest
import org.modelix.services.workspaces.stubs.models.WorkspaceArtifact
import org.modelix.services.workspaces.stubs.models.WorkspaceArtifactList
import org.modelix.workspaces.WorkspacesPermissionSchema
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Receives the results of external (CI) builds. See [WorkspaceArtifactStore].
 */
class WorkspaceArtifactsController(
    val artifactStore: WorkspaceArtifactStore,
    val jwtUtil: ModelixJWTUtil,
    val workspaceExists: (workspaceId: String) -> Boolean,
    val artifactIdsInUse: () -> Set<String>,
    val maxUploadSizeBytes: Long,
) {
    companion object {
        const val DEFAULT_TOKEN_VALIDITY_DAYS = 90
        const val MAX_TOKEN_VALIDITY_DAYS = 365
    }

    fun install(route: Route) {
        route.install_()
    }

    private fun Route.install_() {
        modelixWorkspacesWorkspacesArtifactsRoutes(object : ModelixWorkspacesWorkspacesArtifactsController {
            override suspend fun listArtifacts(workspaceId: String, call: TypedApplicationCall<WorkspaceArtifactList>) {
                call.checkPermission(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).buildResult.read)
                if (!call.checkWorkspaceExists(workspaceId)) return
                call.respondTyped(WorkspaceArtifactList(artifactStore.list(workspaceId)))
            }

            override suspend fun getArtifact(workspaceId: String, artifactId: String, call: TypedApplicationCall<WorkspaceArtifact>) {
                call.checkPermission(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).buildResult.read)
                val artifact = artifactStore.get(workspaceId, artifactId)
                if (artifact == null) {
                    call.respond(HttpStatusCode.NotFound, "Artifact not found: $artifactId")
                } else {
                    call.respondTyped(artifact)
                }
            }

            override suspend fun deleteArtifact(workspaceId: String, artifactId: String, call: ApplicationCall) {
                call.checkPermission(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).buildResult.write)
                if (artifactStore.delete(workspaceId, artifactId)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Artifact not found: $artifactId")
                }
            }
        })

        modelixWorkspacesWorkspacesArtifactsContentZipRoutes(object : ModelixWorkspacesWorkspacesArtifactsContentZipController {
            override suspend fun downloadArtifact(workspaceId: String, artifactId: String, call: TypedApplicationCall<ByteArray>) {
                call.checkPermission(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).buildResult.read)
                val file = artifactStore.getContentFile(workspaceId, artifactId)
                if (file == null) {
                    call.respond(HttpStatusCode.NotFound, "Artifact not found: $artifactId")
                } else {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "$artifactId.zip").toString(),
                    )
                    call.respondFile(file)
                }
            }
        })

        modelixWorkspacesWorkspacesArtifactUploadTokenRoutes(object : ModelixWorkspacesWorkspacesArtifactUploadTokenController {
            override suspend fun createArtifactUploadToken(
                workspaceId: String,
                artifactUploadTokenRequest: ArtifactUploadTokenRequest?,
                call: TypedApplicationCall<ArtifactUploadToken>,
            ) {
                // Intentionally stricter than build-result/write. Otherwise, a token could be used to create new tokens
                // and by that extend its own lifetime.
                call.checkPermission(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).config.write)
                if (!call.checkWorkspaceExists(workspaceId)) return
                val validityDays = (artifactUploadTokenRequest?.validityDays ?: DEFAULT_TOKEN_VALIDITY_DAYS)
                    .coerceIn(1, MAX_TOKEN_VALIDITY_DAYS)
                val expiresAt = Instant.now().plus(validityDays.toLong(), ChronoUnit.DAYS)
                val token = jwtUtil.createAccessToken(
                    call.getUserName() ?: "artifact-upload@modelix.org",
                    listOf(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).buildResult.write.fullId),
                ) {
                    it.claimSetBuilder.expirationTime(Date.from(expiresAt))
                }
                call.respondTyped(ArtifactUploadToken(token = token, expiresAt = expiresAt.toString()))
            }
        })

        // Not using the generated route, because it would load the whole request body into memory.
        authenticate("modelixJwtAuth") {
            post("/modelix/workspaces/workspaces/{workspaceId}/artifacts/upload") {
                val workspaceId = call.parameters["workspaceId"]!!
                call.checkPermission(WorkspacesPermissionSchema.workspaces.workspace(workspaceId).buildResult.write)
                if (!call.checkWorkspaceExists(workspaceId)) return@post

                val contentLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                if (contentLength != null && contentLength > maxUploadSizeBytes) {
                    call.respond(HttpStatusCode.PayloadTooLarge, "Artifact exceeds the maximum size of $maxUploadSizeBytes bytes")
                    return@post
                }

                val artifact = try {
                    withContext(Dispatchers.IO) {
                        call.receiveStream().use { input ->
                            artifactStore.store(
                                workspaceId = workspaceId,
                                input = input,
                                uploadedBy = call.getUserName(),
                                maxSizeBytes = maxUploadSizeBytes,
                                protectedArtifactIds = artifactIdsInUse,
                            )
                        }
                    }
                } catch (ex: ArtifactTooLargeException) {
                    call.respond(HttpStatusCode.PayloadTooLarge, ex.message.orEmpty())
                    return@post
                } catch (ex: InvalidArtifactException) {
                    call.respond(HttpStatusCode.BadRequest, ex.message.orEmpty())
                    return@post
                }
                call.respond(HttpStatusCode.Created, artifact)
            }
        }
    }

    private suspend fun ApplicationCall.checkWorkspaceExists(workspaceId: String): Boolean {
        if (workspaceExists(workspaceId)) return true
        respond(HttpStatusCode.NotFound, "Workspace not found: $workspaceId")
        return false
    }
}
