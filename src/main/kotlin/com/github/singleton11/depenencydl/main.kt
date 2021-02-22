package com.github.singleton11.depenencydl

import com.github.singleton11.depenencydl.core.DependencyIndexBuilder
import com.github.singleton11.depenencydl.integration.ModelDependencyResolver
import com.github.singleton11.depenencydl.integration.SimpleHttpResolver
import com.github.singleton11.depenencydl.integration.util.RepositoryHelper
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Repositories
import com.github.singleton11.depenencydl.model.Repository
import com.github.singleton11.depenencydl.persistence.SemVerDependencyConflictResolver
import com.github.singleton11.depenencydl.persistence.TreeDependencyIndex
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import org.apache.maven.model.Model
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.building.ModelProblemCollector
import org.apache.maven.model.validation.ModelValidator
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager


fun main() {

    HttpClient(CIO) {
        engine {
            maxConnectionsCount = 10
            endpoint {
                connectAttempts = 10
            }
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        // Do nothing
                    }

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        // Do nothing
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                }
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

        val simpleHttpResolver = SimpleHttpResolver(
            httpClient,
            repositories,
            listOf(
                Artifact("xml-apis", "xml-apis", "2.6.2") to Artifact("xerces", "xmlParserAPIs", "2.6.2"),
                Artifact("xerces", "xerces-impl", "2.6.2") to Artifact("xerces", "xercesimpl", "2.6.2"),
                Artifact("stax", "stax-ri", "1.0") to Artifact("stax", "stax", "1.2.0")
            )
        )
        val defaultModelBuilder = DefaultModelBuilderFactory().newInstance().setModelValidator(object : ModelValidator {
            override fun validateRawModel(
                model: Model?,
                request: ModelBuildingRequest?,
                problems: ModelProblemCollector?
            ) {
                // Do nothing
            }

            override fun validateEffectiveModel(
                model: Model?,
                request: ModelBuildingRequest?,
                problems: ModelProblemCollector?
            ) {
                // Do nothing
            }

        })

        val modelDependencyResolver = ModelDependencyResolver(
            defaultModelBuilder,
            simpleHttpResolver
        )


//        val resolveDependencies = modelDependencyResolver.resolveDependencies(Artifact("com.googlecode.jmockit", "jmockit", "1.6"))
//        println(resolveDependencies)

        val dependencyIndexBuilder =
            DependencyIndexBuilder(TreeDependencyIndex(SemVerDependencyConflictResolver()), modelDependencyResolver)
        runBlocking {
            dependencyIndexBuilder.build(
                listOf(
                    Artifact(
                        "org.jfrog.buildinfo",
                        "build-info-extractor-gradle",
                        "4.20.0"
                    )
                )
            )
//            dependencyIndexBuilder.build(listOf(Artifact("com.google.guava", "guava", "30.1-jre")))

            println(dependencyIndexBuilder)
        }
    }
}
