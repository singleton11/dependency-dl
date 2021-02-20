package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.integration.RepositoryClient
import com.github.singleton11.depenencydl.model.Dependency
import com.github.singleton11.depenencydl.persistence.DependencyIndex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging

class DependencyIndexBuilder(
    private val dependencyIndex: DependencyIndex,
    private val repositoryClient: RepositoryClient
) {

    private val logger = KotlinLogging.logger { }

    suspend fun buildDependencyIndex(
        dependencies: List<Dependency>
    ) {
        val channel = Channel<Pair<Dependency, Dependency?>>()
        GlobalScope.launch {
            handleDependency(channel)
        }
        for (dependency in dependencies) {
            channel.send(dependency to null)
        }
    }

    private suspend fun handleDependency(channel: Channel<Pair<Dependency, Dependency?>>) {
        for (dependencyPair in channel) {
            val (dependency, parentDependency) = dependencyPair
            logger.debug { "Handling dependency $dependency" }
            dependencyIndex.add(dependency, parentDependency)
            GlobalScope.launch {
                val children = repositoryClient.getDependencies(dependency)
                for (child in children) {
                    channel.send(child to dependency)
                }
            }
        }
    }
}