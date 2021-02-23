package com.github.singleton11.depenencydl.integration

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import com.github.singleton11.depenencydl.integration.exception.DependencyNotFoundException
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Event
import com.github.singleton11.depenencydl.model.RepositoryAddedEvent
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.maven.model.Dependency
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.ModelSource2
import org.apache.maven.model.resolution.ModelResolver
import java.io.File
import java.net.ConnectException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.UnresolvedAddressException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*


class SimpleHttpResolver(
    private val httpClient: HttpClient,
    val repositories: MutableList<Repository> = Collections.synchronizedList(mutableListOf()),
    private val manualReplacements: List<Pair<Artifact, Artifact>> = listOf(),
    private val channel: Channel<Event>
) : ModelResolver {

    private val logger = KotlinLogging.logger { }
    private var internalRepositories = listOf<Repository>()
    private val internalCache = mutableMapOf<Triple<String, String, String>, String>()
    private val internalManualReplacements: Map<Triple<String, String, String>, Triple<String, String, String>>

    init {
        internalRepositories = repositories
        val folder = File("poms/")
        folder.mkdirs()
        internalManualReplacements = manualReplacements
            .map {
                Triple(it.first.groupId, it.first.artifactId, it.first.version) to Triple(
                    it.second.groupId,
                    it.second.artifactId,
                    it.second.version
                )
            }
            .toMap()
    }

    override fun resolveModel(groupId: String, artifactId: String, version: String) =
        internalManualReplacements[Triple(groupId, artifactId, version)]?.let {
            internalResolveModel(it.first, it.second, it.third)
        } ?: kotlin.run {
            internalResolveModel(groupId, artifactId, version)
        }

    private fun internalResolveModel(groupId: String, artifactId: String, version: String): ModelSource2 {
        internalCache[Triple(groupId, artifactId, version)]?.let {
            val file = File(it)
            return FileModelSource(file)
        } ?: kotlin.run {
            val filePath = "poms/$groupId-$artifactId-$version.xml"
            val localFile = File(filePath)
            if (localFile.exists()) {
                internalCache[Triple(groupId, artifactId, version)] = filePath
                return FileModelSource(localFile)
            }
            for (repository in internalRepositories) {
                val repositoryUrl = repository.url
                val pomPath = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
                val urlString = "$repositoryUrl$pomPath"
                try {
                    logger.debug { "Getting artifact metadata $urlString" }
                    val pom = runBlocking {
                        val retryTimeouts: RetryPolicy<Throwable> = {
                            if (reason is ConnectException) ContinueRetrying else StopRetrying
                        }
                        retry(retryTimeouts + limitAttempts(50) + binaryExponentialBackoff(base = 10L, max = 10000L)) {
                            httpClient.get<String>(urlString)
                        }
                    }
                    logger.debug { "Got artifact metadata $urlString" }
                    val file = File(filePath)
                    FileChannel
                        .open(Path.of(filePath), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                        .use { channel ->
                            val buff: ByteBuffer = ByteBuffer.wrap(pom.toByteArray())
                            channel.write(buff)
                        }
                    logger.debug { "Artifact metadata saved to ${file.toURI()}" }
                    internalCache[Triple(groupId, artifactId, version)] = filePath
                    return FileModelSource(file)
                } catch (e: ClientRequestException) {
                    logger.debug(
                        "Dependency {} not found in repository {}",
                        "$groupId:$artifactId:$version",
                        repositoryUrl
                    )
                } catch (e: UnresolvedAddressException) {
                    logger.debug("Unresolved address in dependency {}", "$groupId:$artifactId:$version", repositoryUrl)
                }
            }
        }
        throw DependencyNotFoundException(groupId, artifactId, version)
    }

    override fun resolveModel(parent: Parent): ModelSource2 {
        val artifactId = parent.artifactId
        val groupId = parent.groupId
        val version = parent.version
        return resolveModel(groupId, artifactId, version)
    }

    override fun resolveModel(dependency: Dependency): ModelSource2 {
        val artifactId = dependency.artifactId
        val groupId = dependency.groupId
        val version = dependency.version
        return resolveModel(groupId, artifactId, version)
    }

    override fun addRepository(repository: Repository) {
        internalRepositories = internalRepositories + repository
        runBlocking {
            channel.send(
                RepositoryAddedEvent(
                    com.github.singleton11.depenencydl.model.Repository(
                        repository.id,
                        repository.url
                    ), false
                )
            )
        }
    }

    override fun addRepository(repository: Repository, replace: Boolean) {
        internalRepositories = internalRepositories.filter { it.url != repository.url } + repository
        runBlocking {
            channel.send(
                RepositoryAddedEvent(
                    com.github.singleton11.depenencydl.model.Repository(
                        repository.id,
                        repository.url
                    ), true
                )
            )
        }
    }

    override fun newCopy(): ModelResolver {
        return SimpleHttpResolver(httpClient, internalRepositories.toMutableList(), channel = channel)
    }
}