package com.github.singleton11.depenencydl

import com.github.singleton11.depenencydl.core.DependencyIndexBuilder
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
import com.github.singleton11.depenencydl.persistence.wol.WriteAheadLogService
import com.github.singleton11.depenencydl.util.NoopTrustManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.runBlocking
import org.apache.maven.model.building.DefaultModelBuilderFactory


fun main() {

    HttpClient(CIO) {
        engine {
            maxConnectionsCount = 20
            pipelining = true
            endpoint {
                connectAttempts = 10
            }
            https {
                trustManager = NoopTrustManager()
            }
        }
    }.use { httpClient ->

        val repositories = RepositoryHelper.getInternalMavenRepositories(
            Repositories(
                listOf(
                    Repository(
                        "central",
                        "https://repo.maven.apache.org/maven2/"
                    ),
                    Repository(
                        "gradle-plugins",
                        "https://plugins.gradle.org/m2/"
                    )
                )
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

        val modelDependencyResolver = ModelDependencyResolver(
            defaultModelBuilder,
            simpleHttpResolver
        )

//        val resolveDependencies =
//            modelDependencyResolver.resolveDependencies(Artifact("org.apache.velocity", "velocity", "1.6.2"))
//        println(resolveDependencies)

        val artifacts = listOf(
            Artifact(
                "org.jfrog.buildinfo",
                "build-info-extractor-gradle",
                "4.20.0"
            )
        )

        val writeAheadLogService = WriteAheadLogService(artifacts)

        val dependencyIndexBuilder =
            DependencyIndexBuilder(
                TreeDependencyIndex(SemVerDependencyConflictResolver()),
                modelDependencyResolver,
                writeAheadLogService,
                channel
            )
        runBlocking {
            dependencyIndexBuilder.build(artifacts)
            // Download dependencies
            println(dependencyIndexBuilder)
        }
    }
}
