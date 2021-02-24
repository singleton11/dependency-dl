package com.github.singleton11.depenencydl

import com.github.singleton11.depenencydl.core.DependencyDownloader
import com.github.singleton11.depenencydl.core.DependencyIndexBuilder
import com.github.singleton11.depenencydl.integration.ModelDependencyResolver
import com.github.singleton11.depenencydl.integration.NoopModelValidator
import com.github.singleton11.depenencydl.integration.SimpleHttpResolver
import com.github.singleton11.depenencydl.integration.util.RepositoryHelper
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Repositories
import com.github.singleton11.depenencydl.model.Repository
import com.github.singleton11.depenencydl.persistence.SemVerDependencyConflictResolver
import com.github.singleton11.depenencydl.persistence.TreeDependencyIndex
import com.github.singleton11.depenencydl.persistence.jar.JarDownloader
import io.ktor.client.*
import io.ktor.client.engine.apache.*
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

    val manualReplacements = listOf(
        Artifact("xml-apis", "xml-apis", "2.6.2") to Artifact("xerces", "xmlParserAPIs", "2.6.2"),
        Artifact("xerces", "xerces-impl", "2.6.2") to Artifact("xerces", "xercesimpl", "2.6.2"),
        Artifact("stax", "stax-ri", "1.0") to Artifact("stax", "stax", "1.2.0"),
        Artifact("org.jvnet.staxex", "stax-ex", "RELEASE") to Artifact("org.jvnet.staxex", "stax-ex", "2.0.0"),
        Artifact("com.sun.istack", "istack-commons-runtime", "1.1") to Artifact(
            "com.sun.istack",
            "istack-commons-runtime",
            "2.20"
        )
    )

    val repositories = RepositoryHelper.getInternalMavenRepositories(
        Repositories(coreRepositories).listOfPairs()
    ).toMutableList()

    HttpClient(Apache) {
        engine {
            customizeClient {
                setSSLHostnameVerifier { _, _ ->
                    true
                }
            }
        }
    }.use { httpClient ->

        val simpleHttpResolver = SimpleHttpResolver(
            httpClient,
            repositories,
        )
        val defaultModelBuilder = DefaultModelBuilderFactory()
            .newInstance()
            .setModelValidator(NoopModelValidator())
        val modelDependencyResolver = ModelDependencyResolver(defaultModelBuilder, simpleHttpResolver)
        val dependencyIndex = TreeDependencyIndex(SemVerDependencyConflictResolver())
        val dependencyIndexBuilder =
            DependencyIndexBuilder(dependencyIndex, modelDependencyResolver, manualReplacements)
        val jarDownloader = JarDownloader(httpClient)
        val dependencyDownloader = DependencyDownloader(dependencyIndexBuilder, jarDownloader)
        runBlocking {
            dependencyDownloader.run(artifacts)
        }
    }
}
