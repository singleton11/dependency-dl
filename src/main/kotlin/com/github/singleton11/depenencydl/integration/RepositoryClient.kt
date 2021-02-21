package com.github.singleton11.depenencydl.integration

import com.github.singleton11.depenencydl.integration.exception.DependencyNotFoundException
import com.github.singleton11.depenencydl.integration.exception.DependencyVersionCantBeResolvedException
import com.github.singleton11.depenencydl.integration.model.Project
import com.github.singleton11.depenencydl.model.Dependency
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import mu.KotlinLogging

class RepositoryClient(private val httpClient: HttpClient, private val repositories: List<String>) {

    private val logger = KotlinLogging.logger { }

    suspend fun getDependencies(dependency: Dependency): List<Dependency> {
//        val retryTimeouts: RetryPolicy<Throwable> = {
//            if (reason is ClientRequestException) ContinueRetrying else StopRetrying
//        }
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
                return dependencies.map { dependencyValue ->
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
        return resolveDependencyVersionsProject
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
}