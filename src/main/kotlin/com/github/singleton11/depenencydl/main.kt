package com.github.singleton11.depenencydl

import com.github.singleton11.depenencydl.core.DependencyDownloader
import com.github.singleton11.depenencydl.core.DependencyIndexBuilder
import com.github.singleton11.depenencydl.core.StateRestoring
import com.github.singleton11.depenencydl.integration.ModelDependencyResolver
import com.github.singleton11.depenencydl.integration.NoopModelValidator
import com.github.singleton11.depenencydl.integration.SimpleHttpResolver
import com.github.singleton11.depenencydl.integration.util.RepositoryHelper
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Event
import com.github.singleton11.depenencydl.model.Repositories
import com.github.singleton11.depenencydl.model.Repository
import com.github.singleton11.depenencydl.persistence.SemVerDependencyConflictResolver
import com.github.singleton11.depenencydl.persistence.TreeDependencyIndex
import com.github.singleton11.depenencydl.persistence.jar.JarDownloader
import com.github.singleton11.depenencydl.persistence.wol.WriteAheadLogService
import com.github.singleton11.depenencydl.util.NoopTrustManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.runBlocking
import org.apache.maven.model.building.DefaultModelBuilderFactory
import java.io.File


fun main() {

    val artifacts = File("dependencies.txt")
        .readLines()
        .map { it.split(":") }
        .map { Artifact(it[0], it[1], it[2]) }
    val coreRepositories = File("repositories.txt")
        .readLines()
        .map { it.split(" ") }
        .map { Repository(it[0], it[1]) }

    HttpClient(CIO) {
        engine {
            maxConnectionsCount = 20
            pipelining = true
            requestTimeout = 1000
            endpoint {
                connectAttempts = 10
                connectTimeout = 1000
            }
            https {
                trustManager = NoopTrustManager()
            }
        }
    }.use { httpClient ->

        val repositories = RepositoryHelper.getInternalMavenRepositories(
            Repositories(
                coreRepositories
            ).listOfPairs()
        ).toMutableList()

        val channel = Channel<Event>(UNLIMITED)

        val simpleHttpResolver = SimpleHttpResolver(
            httpClient,
            repositories,
            listOf(
                Artifact("xml-apis", "xml-apis", "2.6.2") to Artifact("xerces", "xmlParserAPIs", "2.6.2"),
                Artifact("xerces", "xerces-impl", "2.6.2") to Artifact("xerces", "xercesimpl", "2.6.2"),
                Artifact("stax", "stax-ri", "1.0") to Artifact("stax", "stax", "1.2.0"),
                Artifact("org.jvnet.staxex", "stax-ex", "RELEASE") to Artifact("org.jvnet.staxex", "stax-ex", "2.0.0")
            ),
            channel
        )
        val defaultModelBuilder = DefaultModelBuilderFactory()
            .newInstance()
            .setModelValidator(NoopModelValidator())

        val modelDependencyResolver = ModelDependencyResolver(defaultModelBuilder, simpleHttpResolver)

        val writeAheadLogService = WriteAheadLogService(artifacts)
        val dependencyIndex = TreeDependencyIndex(SemVerDependencyConflictResolver())
        val dependencyIndexBuilder =
            DependencyIndexBuilder(
                dependencyIndex,
                modelDependencyResolver,
                writeAheadLogService,
                channel
            )
        val jarDownloader = JarDownloader(httpClient)
        val stateRestoring = StateRestoring(dependencyIndex, simpleHttpResolver)
        val dependencyDownloader = DependencyDownloader(stateRestoring, dependencyIndexBuilder, jarDownloader)
        runBlocking {
            dependencyDownloader.run(artifacts)
        }
    }
}
