package com.github.singleton11.depenencydl.integration

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
import com.github.michaelbull.retry.retry
import com.github.singleton11.depenencydl.integration.exception.DependencyNotFoundException
import com.github.singleton11.depenencydl.integration.exception.DependencyVersionCantBeResolvedException
import com.github.singleton11.depenencydl.integration.model.Project
import com.github.singleton11.depenencydl.model.Dependency
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import mu.KotlinLogging
import java.net.ConnectException

class RepositoryClient(
    private val httpClient: HttpClient,
    private val repositories: List<String>,
    manualReplacement: List<Pair<Dependency, Dependency>>
) {
    private val internalManualReplacement: Map<com.github.singleton11.depenencydl.integration.model.Dependency,
            com.github.singleton11.depenencydl.integration.model.Dependency> =
        manualReplacement.map {
            val dependency1 = it.first
            val dependency2 = it.second
            val converted1 = com.github.singleton11.depenencydl.integration.model.Dependency(
                dependency1.groupId,
                dependency1.artifactId,
                dependency1.version
            )
            val converted2 = com.github.singleton11.depenencydl.integration.model.Dependency(
                dependency2.groupId,
                dependency2.artifactId,
                dependency2.version
            )
            converted1 to converted2
        }.toMap()

    private val logger = KotlinLogging.logger { }

    suspend fun getDependencies(dependency: Dependency): List<Dependency> {
        val retryTimeouts: RetryPolicy<Throwable> = {
            if (reason is ConnectException) ContinueRetrying else StopRetrying
        }
        return retry(retryTimeouts) {
            for (repository in repositories) {
                try {
                    val response = getPomForDependency(dependency, repository)
                    var dependencies = response.dependencies ?: emptyList()
                    var parent = response.parent
                    while (parent?.version != null) {
                        val shouldResolveParent = dependencies.any { it.version == null }
                        if (shouldResolveParent) {
                            logger.debug { "Dependencies with unresolved versions found for dependency $dependency" }
                            // Get parent dependency management response
                            val parentResponse = getPomForDependency(
                                Dependency(
                                    parent.groupId,
                                    parent.artifactId,
                                    parent.version!!
                                ),
                                repository
                            )

                            // Build version map
                            val versionMap = parentResponse.dependencyManagement?.dependencies
                                ?.map { (it.groupId to it.artifactId) to it.version }
                                ?.toMap() ?: emptyMap()

                            logger.debug { "Dependency management versions map is $versionMap for dependency $dependency" }

                            // Populate versions from this map
                            dependencies = dependencies.map { dependencyValue ->
                                dependencyValue.version?.let { dependencyValue } ?: kotlin.run {
                                    versionMap[dependencyValue.groupId to dependencyValue.artifactId]?.let { dependencyValueVersion ->
                                        dependencyValue.copy(version = dependencyValueVersion)
                                    } ?: dependencyValue
                                }
                            }

                            parent = parentResponse.parent
                        } else {
                            break
                        }
                    }

                    // Map dependencies
                    return@retry dependencies.map { dependencyValue ->
                        dependencyValue.version?.let { dependencyValueVersion ->
                            Dependency(dependencyValue.groupId, dependencyValue.artifactId, dependencyValueVersion)
                        } ?: throw DependencyVersionCantBeResolvedException(dependencyValue)
                    }
                } catch (e: ClientRequestException) {
                    logger.debug("Artifact {} not found in repository {}", dependency, repository)
                }
            }
            throw DependencyNotFoundException(dependency)
        }
    }

    private suspend fun getPomForDependency(
        dependency: Dependency,
        repository: String
    ): Project {
        val (groupId, artifactId, version) = dependency
        val pomPath = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
        val urlString = "$repository$pomPath"
        logger.debug { "Getting dependencies using url: $urlString" }
        val project = httpClient.get<Project>(urlString)
        logger.debug { "Project obtained $project" }
        val resolveDependencyVersionsProject = resolveDependencyVersions(dependency, project)
        logger.debug { "Project with resolved dependency versions $resolveDependencyVersionsProject" }
        val replacedDependenciesProject = applyManualReplacements(resolveDependencyVersionsProject)
        logger.debug { "Project with manual replaced dependencies $replacedDependenciesProject" }
        return replacedDependenciesProject
    }

    private fun resolveDependencyVersions(dependency: Dependency, project: Project): Project {
        val resolvedDependencies = resolveDependencies(dependency, project, project.dependencies)
        val resolvedDependenciesForDependencyManagement = resolveDependencies(
            dependency,
            project,
            project.dependencyManagement?.dependencies
        )
        val resolvedDependencyManagement = project.dependencyManagement?.copy(
            dependencies = resolvedDependenciesForDependencyManagement ?: emptyList()
        )
        return project.copy(dependencies = resolvedDependencies, dependencyManagement = resolvedDependencyManagement)
    }

    private fun resolveDependencies(
        dependency: Dependency,
        project: Project,
        dependencies: List<com.github.singleton11.depenencydl.integration.model.Dependency>?
    ) = dependencies?.map { dependencyValue ->
        val resolvedVersion = dependencyValue.version?.let { version ->
            when {
                version == "\${project.version}" -> {
                    dependency.version
                }
                Regex("\\$\\{.*}").matches(version) -> {
                    val variable = version.substring(2 until version.length - 1)
                    project.properties?.let { properties ->
                        properties[variable]
                    }
                }
                else -> {
                    version
                }
            }
        }
        dependencyValue.copy(version = resolvedVersion)
    }

    private fun applyManualReplacements(project: Project): Project {
        val replacedDependencies = project.dependencies?.map { dependency ->
            internalManualReplacement[dependency] ?: dependency
        }
        val replacedDependenciesForDependencyManagement = project
            .dependencyManagement
            ?.dependencies
            ?.map { dependency -> internalManualReplacement[dependency] ?: dependency } ?: emptyList()
        val replacedDependencyManagement = project
            .dependencyManagement
            ?.copy(dependencies = replacedDependenciesForDependencyManagement)

        return project.copy(dependencies = replacedDependencies, dependencyManagement = replacedDependencyManagement)
    }
}