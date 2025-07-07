import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.http.server.GitServlet
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.modelix.authorization.ModelixJWTUtil
import org.modelix.authorization.permissions.PermissionSchemaBase
import org.modelix.model.api.BuiltinLanguages
import org.modelix.model.api.getDescendants
import org.modelix.model.api.getName
import org.modelix.model.client2.ModelClientV2
import org.modelix.model.client2.runWriteOnModel
import org.modelix.model.historyAsSequence
import org.modelix.model.lazy.CLVersion
import org.modelix.model.lazy.RepositoryId
import org.modelix.model.mutable.ModelixIdGenerator
import org.modelix.model.mutable.asModelSingleThreaded
import org.openapitools.client.apis.DraftsApi
import org.openapitools.client.apis.GitBranchesApi
import org.openapitools.client.apis.GitRepositoriesApi
import org.openapitools.client.models.DraftConfig
import org.openapitools.client.models.DraftPreparationJob
import org.openapitools.client.models.DraftRebaseJob
import org.openapitools.client.models.GitRemoteConfig
import org.openapitools.client.models.GitRepositoryConfig
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


class DraftRebaseTest {

    @Test
    fun `changes from git can be merged after draft creation`(): Unit = runBlocking { withTimeout(10.minutes) {
        runWithGitRepository { gitUrl ->
            println("gitUrl: $gitUrl")

            val expectedRefs = Git.lsRemoteRepository().setRemote(gitUrl).call().toList()
                .filter { it.name.startsWith("refs/heads/") }
                .associate { it.name.removePrefix("refs/heads/") to it.objectId.name }

            val jwtUtil = ModelixJWTUtil()
            jwtUtil.loadKeysFromFiles(File("build/private-key.pem"))
            fun createAccessToken() = jwtUtil.createAccessToken(
                user = "integration-test@modelix.org",
                grantedPermissions = listOf(PermissionSchemaBase.cluster.admin.fullId),
            )

            val httpClientConfig: (HttpClientConfig<*>) -> Unit = { config ->
                config.apply {
                    expectSuccess = true
                    install(ContentNegotiation) {
                        json(Json {
                            encodeDefaults = true
                            isLenient = true
                            allowSpecialFloatingPointValues = true
                            allowStructuredMapKeys = true
                            prettyPrint = false
                            useArrayPolymorphism = false
                            serializersModule = SerializersModule {
                                contextual(UUID::class, UUIDSerializer())
                            }
                        })
                    }
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.HEADERS
                    }
                    defaultRequest {
                        bearerAuth(createAccessToken())
                        url("https://ktor.io/docs/")
                    }
                }

            }
            val baseUrl = System.getenv("MODELIX_BASE_URL") ?: "http://localhost"
            val repositoriesApi = GitRepositoriesApi(baseUrl = baseUrl, httpClientConfig = httpClientConfig)
            val branchesApi = GitBranchesApi(baseUrl = baseUrl, httpClientConfig = httpClientConfig)
            val draftsApi = DraftsApi(baseUrl = baseUrl, httpClientConfig = httpClientConfig)

            // create new git repository
            val repositoryUUID = UUID.randomUUID()
            val createdRepository = repositoriesApi.createGitRepository(
                GitRepositoryConfig(
                    id = repositoryUUID,
                    name = "git-import-test-repo",
                    remotes = listOf(
                        GitRemoteConfig(
                            name = "origin",
                            url = gitUrl,
                            hasCredentials = false,
                        )
                    ),
                    modelixRepository = repositoryUUID.toString(),
                )
            ).body()

            // branch list is initially empty
            assertEquals(
                branchesApi.listBranches(createdRepository.id.toString()).body().branches.map { it.name }.toSet(),
                setOf()
            )

            // trigger update of branches list
            val branchesList = branchesApi.updateBranches(createdRepository.id.toString()).body()
            assertEquals(
                setOf("main", "branch-for-draft-rebase", "feature-A", "feature-B"),
                branchesList.branches.map { it.name }.toSet()
            )
            assertEquals(expectedRefs, branchesList.branches.associate { it.name to it.gitCommitHash })

            // create new draft
            val draftUUID = UUID.randomUUID()
            val createdDraft = draftsApi.createDraftInRepository(createdRepository.id.toString(), DraftConfig(
                id = draftUUID.toString(),
                gitRepositoryId = createdRepository.id.toString(),
                gitBranchName = "branch-for-draft-rebase",
                baseGitCommit = branchesList.branches.first { it.name == "branch-for-draft-rebase" }.gitCommitHash!!,
                modelixBranchName = "draft/$draftUUID",
            )).body()

            assertEquals("aac3b37b4c6213f8057961a79814989b2cbf3007", createdDraft.baseGitCommit)
            assertEquals("drafts/${createdDraft.id}", createdDraft.modelixBranchName)

            // trigger draft branch creation
            draftsApi.prepareDraftBranch(createdDraft.id.toString(), DraftPreparationJob())
            withTimeout(3.minutes) {
                while (draftsApi.getDraftBranchPreparationJob(createdDraft.id.toString()).body().also { it.errorMessage?.let { error(it) } }.active == true) {
                    delay(3.seconds)
                }
            }

            val modelClient = ModelClientV2.builder()
                .url("$baseUrl/model/")
                .authToken { createAccessToken() }
                .build()

            // check that the branch was created
            val repositoryId = RepositoryId(createdRepository.id.toString())
            val draftBranchRef = repositoryId.getBranchReference("drafts/${createdDraft.id}")
            assertContains(modelClient.listRepositories(), repositoryId)
            assertEquals(
                setOf(
                    repositoryId.getBranchReference(),
                    draftBranchRef,
                    repositoryId.getBranchReference("git-import/branch-for-draft-rebase"),
                ),
                modelClient.listBranches(repositoryId).toSet()
            )
            val version1 = modelClient.pull(draftBranchRef, null)
            assertEquals(expectedRefs["branch-for-draft-rebase"], version1.getAttributes()["git-commit"])

            val versionChangedInDraft = modelClient.runWriteOnModel(draftBranchRef) { rootNode ->
                val methodNode = rootNode.getDescendants(true).first { it.getName() == "f" }
                methodNode.setPropertyValue(BuiltinLanguages.jetbrains_mps_lang_core.INamedConcept.name.toReference(), "renamedInDraft")
            }

            // merge changes from main into the draft
            draftsApi.rebaseDraft(createdDraft.id.toString(), DraftRebaseJob(
                gitBranchName = "main",
                baseGitCommit = branchesList.branches.first { it.name == "main" }.gitCommitHash!!
            ))
            withTimeout(3.minutes) {
                while (draftsApi.getDraftRebaseJob(createdDraft.id.toString()).body().also { it.errorMessage?.let { error(it) } }.active == true) {
                    delay(3.seconds)
                }
            }

            // check merge result
            val version2 = modelClient.pull(draftBranchRef, null) as CLVersion
            assertNotEquals(version1.getContentHash(), version2.getContentHash())
            val mergedHistory = version2.historyAsSequence().map { it.getAttributes()["git-commit"] }.toSet()

            // A draft "rebase" currently actually just does a merge and all original commits are still expected to be
            // part of the history.
            assertContains(mergedHistory, "aac3b37b4c6213f8057961a79814989b2cbf3007")
            assertContains(mergedHistory, "153318b1deac5ad0d3e351d624042e6d396005a9")
            assertContains(version2.historyAsSequence().map { it.getObjectHash() }.toSet(), versionChangedInDraft.getObjectHash())

            // The method renamed in the draft should still be there
            assertContains(
                version2.getModelTree().asModelSingleThreaded().getRootNode().getDescendants(true).map { it.getName() },
                "renamedInDraft"
            )

            // cleanup
            repositoriesApi.deleteGitRepository(repositoryId.toString())
            modelClient.deleteRepository(repositoryId)
        }}
    }

    suspend fun <R> runWithGitRepository(body: suspend (url: String) -> R): R {
        val gitServlet = GitServlet()

        extractZip("git-import-test-repo.zip", "build/git-import-test-repo")
        File("build/git-import-test-repo/git-import-test-repo/.git/refs/heads/branch-for-draft-rebase").writeText("aac3b37b4c6213f8057961a79814989b2cbf3007")

        gitServlet.setRepositoryResolver { req, name ->
            FileRepositoryBuilder()
                .setGitDir(File("build/git-import-test-repo/git-import-test-repo/.git"))
                //.readEnvironment()
                //.findGitDir()
                .build()
        }

        val context = ServletContextHandler()
        context.setContextPath("/")
        context.addServlet(ServletHolder(gitServlet), "/git/*")

        val server = Server()
        val connector = ServerConnector(server)
        connector.port = 0 // 0 = automatically assign a free port
        server.addConnector(connector)
        server.setHandler(context)
        server.start()
        //server.join()

        try {
            val inetAddress = NetworkInterface.getNetworkInterfaces().toList()
                .filter { !it.isLoopback && it.isUp }
                .flatMap { it.inetAddresses().toList() }
                .filterIsInstance<Inet4Address>()
                .first()
            val url = "http://${inetAddress.hostAddress}:${connector.localPort}/git/git-import-test-repo"
            return body(url)
        } finally {
            server.stop()
        }

    }
}

class UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

private fun extractZip(zipPath: String, outputDir: String) {
    val destDir = File(outputDir)
    ZipFile(zipPath).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            val outFile = File(destDir, entry.name)

            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile.mkdirs()
                zip.getInputStream(entry).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}