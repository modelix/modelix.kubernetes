package org.modelix.services.gitconnector

import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1EnvVar
import io.kubernetes.client.openapi.models.V1Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.modelix.authorization.ModelixJWTUtil
import org.modelix.model.client2.IModelClientV2
import org.modelix.model.lazy.RepositoryId
import org.modelix.model.server.ModelServerPermissionSchema
import org.modelix.services.gitconnector.stubs.models.GitRepositoryConfig
import org.modelix.services.workspaces.metadata
import org.modelix.services.workspaces.spec
import org.modelix.services.workspaces.template
import org.modelix.workspace.manager.ITaskInstance
import org.modelix.workspace.manager.KubernetesJobTask
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant

class GitExportTask(
    val key: Key,
    scope: CoroutineScope,
    val modelClient: IModelClientV2,
    val jwtUtil: ModelixJWTUtil,
) : ITaskInstance<FetchedBranch>, KubernetesJobTask<FetchedBranch>(scope) {

    companion object {
        @OptIn(ExperimentalTime::class)
        fun timeForBranchName() = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Clock.System.now().toJavaInstant())
    }

    data class Key(
        val repo: GitRepositoryConfig,
        val modelixVersionHash: String,
        val modelixBranchName: String,
        val gitBaseBranch: String,
    )

    private val repoId = requireNotNull(key.repo.modelixRepository?.let { RepositoryId(it) }) { "Repository ID missing" }
    private val modelixBranch = repoId.getBranchReference(key.modelixBranchName)
    val gitBranchName = "modelix-export/${key.gitBaseBranch}/${timeForBranchName()}-${key.modelixVersionHash.replace("*", "")}"

    private fun chooseRemote() = requireNotNull(key.repo.remotes?.firstOrNull()) { "No remotes specified" }

    override fun getResultCheckingInterval(): Duration = 30.seconds

    override suspend fun tryGetResult(): FetchedBranch? {
        val remote = chooseRemote()
        val cmd = Git.lsRemoteRepository()
        cmd.setRemote(remote.url)
        cmd.setHeads(true)
        cmd.setTags(false)

        val username = remote.credentials?.username.orEmpty()
        val password = remote.credentials?.password.orEmpty()
        if (password.isNotEmpty()) {
            cmd.applyCredentials(username, password)
        }
        cmd.configureHttpProxy()

        val refs = withContext(Dispatchers.IO) {
            cmd.call()
        }

        for (ref in refs) {
            if (!ref.name.startsWith("refs/heads/")) continue
            val branchName = ref.name.removePrefix("refs/heads/")
            if (branchName == this.gitBranchName) {
                return FetchedBranch(
                    remoteName = remote.name,
                    branchName = branchName,
                    commitHash = ref.objectId.name,
                )
            }
        }
        return null
    }

    @Suppress("ktlint")
    override fun generateJobYaml(): V1Job {
        val remote = chooseRemote()
        val token = jwtUtil.createAccessToken(
            "git-sync@modelix.org",
            listOf(
                ModelServerPermissionSchema.repository(modelixBranch.repositoryId).read.fullId,
                ModelServerPermissionSchema.branch(modelixBranch).read.fullId,
            ),
        )

        return V1Job().apply {
            metadata {
                name = "gitexportjob-$id"
            }
            spec {
                template {
                    spec {
                        addContainersItem(V1Container().apply {
                            name = "importer"
                            image = System.getenv("GIT_IMPORT_IMAGE")
                            System.getenv("MODELIX_HTTP_PROXY")?.let {
                                addEnvItem(V1EnvVar().name("MODELIX_HTTP_PROXY").value(it))
                            }
                            args = listOf(
                                "git-export-remote",
                                remote.url,
                                "--git-user",
                                remote.credentials?.username,
                                "--git-password",
                                remote.credentials?.password,
                                "--model-server",
                                System.getenv("model_server_url"),
                                "--token",
                                token,
                                "--modelix-repository",
                                modelixBranch.repositoryId.id,
                                "--modelix-branch",
                                modelixBranch.branchName,
                                "--version",
                                key.modelixVersionHash,
                                "--git-branch",
                                gitBranchName,
                            )
                        })
                    }
                }
            }
        }
    }
}
