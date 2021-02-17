package com.github.singleton11.depenencydl.integration

import com.github.singleton11.depenencydl.integration.converter.DependencyConverter
import com.github.singleton11.depenencydl.integration.exception.DependencyNotFoundException
import com.github.singleton11.depenencydl.integration.model.Project
import com.github.singleton11.depenencydl.model.Dependency
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import mu.KotlinLogging

class RepositoryClient(private val httpClient: HttpClient, private val repositories: List<String>) {

    private val logger = KotlinLogging.logger { }

    suspend fun getDependencies(dependency: Dependency): List<Dependency> {
        for (repository in repositories) {
            val (groupId, artifactId, version) = dependency
            val pomPath = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
            try {
                val response = httpClient.get<Project>("$repository$pomPath")
                return response.dependencies.map { DependencyConverter().convert(it) }
            } catch (e: ClientRequestException) {
                logger.debug("Artifact {} not found in repository {}", dependency, repository)
            }
        }
        throw DependencyNotFoundException(dependency)
    }
}