package com.github.singleton11.depenencydl.integration

import com.github.singleton11.depenencydl.integration.exception.DependencyNotFoundException
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.http.ConnectionClosedException
import org.apache.maven.model.Dependency
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.ModelSource2
import org.apache.maven.model.resolution.ModelResolver
import java.io.File
import java.net.BindException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.UnresolvedAddressException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*


class SimpleHttpResolver(
    private val httpClient: HttpClient,
    val repositories: MutableList<Repository> = Collections.synchronizedList(mutableListOf()),
) : ModelResolver {

    private val logger = KotlinLogging.logger { }
    private var internalRepositories = listOf<Repository>()

    init {
        internalRepositories = repositories
        val folder = File("poms/")
        folder.mkdirs()
    }

    override fun resolveModel(groupId: String, artifactId: String, version: String): ModelSource2 {
        val filePath = "poms/$groupId-$artifactId-$version.xml"
        val localFile = File(filePath)
        if (localFile.exists()) {
            return FileModelSource(localFile)
        }
        for (repository in internalRepositories) {
            val repositoryUrl = repository.url
            val pomPath = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
            val urlString = "$repositoryUrl$pomPath"
            try {
                logger.debug { "Getting artifact metadata $urlString" }
                val pom = runBlocking {
                    httpClient.get<String>(urlString)
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
                return FileModelSource(file)
            } catch (e: ClientRequestException) {
                logger.debug(
                    "Dependency {} not found in repository {}",
                    "$groupId:$artifactId:$version",
                    repositoryUrl
                )
            } catch (e: UnresolvedAddressException) {
                logger.warn("Unresolved address in dependency {}", "$groupId:$artifactId:$version", repositoryUrl)
            } catch (e: BindException) {
                logger.warn(
                    "Can't assign requested address for dependency {}",
                    "$groupId:$artifactId:$version",
                    repositoryUrl
                )
            } catch (e: ConnectionClosedException) {
                logger.warn(
                    "Connection closed",
                    "$groupId:$artifactId:$version",
                    repositoryUrl, e
                )
            } catch (e: UnknownHostException) {
                logger.warn(
                    "Unknown host",
                    "$groupId:$artifactId:$version",
                    repositoryUrl, e
                )
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
    }

    override fun addRepository(repository: Repository, replace: Boolean) {
        internalRepositories = internalRepositories.filter { it.url != repository.url } + repository
    }

    override fun newCopy(): ModelResolver {
        return SimpleHttpResolver(httpClient, internalRepositories.toMutableList())
    }
}