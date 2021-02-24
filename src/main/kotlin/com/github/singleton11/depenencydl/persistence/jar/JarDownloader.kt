package com.github.singleton11.depenencydl.persistence.jar

import com.github.singleton11.depenencydl.core.exception.DependencyJarNotFoundException
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Repository
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class JarDownloader(private val httpClient: HttpClient) {
    private val logger = KotlinLogging.logger { }

    init {
        File("jars/").mkdirs()
    }

    suspend fun download(artifact: Artifact, repositories: List<Repository>) {
        logger.info { "Downloading artifact $artifact" }
        val filePath = "jars/${artifact.groupId}-${artifact.artifactId}-${artifact.version}.jar"
        if (!File(filePath).exists()) {
            val jarPath = "${
                artifact.groupId.replace(
                    '.',
                    '/'
                )
            }/${artifact.artifactId}/${artifact.version}/${artifact.artifactId}-${artifact.version}.jar"
            for (repository in repositories) {
                try {
                    val statement = httpClient.get<HttpStatement>("${repository.url}$jarPath")
                    val response = statement.execute()
                    if (response.status == HttpStatusCode.OK) {
                        FileChannel
                            .open(Path.of("$filePath.part"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                            .use { channel ->
                                while (response.content.availableForRead > 0) {
                                    response.content.read { byteBuffer ->
                                        channel.write(byteBuffer)
                                    }
                                }
                            }
                        File("$filePath.part").renameTo(File(filePath))
                        return
                    }
                } catch (e: ClientRequestException) {
                    if (e.response.status == HttpStatusCode.NotFound) {
                        continue
                    }
                    throw e
                }
            }
            throw DependencyJarNotFoundException(artifact)
        }
    }
}