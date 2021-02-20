package com.github.singleton11.depenencydl.integration

import com.github.michaelbull.retry.ContinueRetrying
import com.github.michaelbull.retry.StopRetrying
import com.github.michaelbull.retry.policy.RetryPolicy
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
//        val retryTimeouts: RetryPolicy<Throwable> = {
//            if (reason is ClientRequestException) ContinueRetrying else StopRetrying
//        }
        for (repository in repositories) {
            val (groupId, artifactId, version) = dependency
            val pomPath = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
            try {
                val urlString = "$repository$pomPath"
                logger.debug { "Getting dependencies using url: $urlString" }
                val response = httpClient.get<Project>(urlString)
                return response.dependencies?.map { DependencyConverter().convert(it) } ?: emptyList()
            } catch (e: ClientRequestException) {
                logger.debug("Artifact {} not found in repository {}", dependency, repository)
            }
        }
        throw DependencyNotFoundException(dependency)
    }
}