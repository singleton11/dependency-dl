package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.integration.ModelDependencyResolver
import com.github.singleton11.depenencydl.model.*
import com.github.singleton11.depenencydl.persistence.DependencyIndex
import com.github.singleton11.depenencydl.persistence.wol.WriteAheadLogService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging

class DependencyIndexBuilder(
    private val dependencyIndex: DependencyIndex,
    private val modelDependencyResolver: ModelDependencyResolver,
    private val writeAheadLogService: WriteAheadLogService,
    private val channel: Channel<Event>,
) {
    private val logger = KotlinLogging.logger { }
    private val repositories: MutableList<Repository> = mutableListOf()

    suspend fun build(artifacts: List<Artifact>) {

        val handleArtifacts = GlobalScope.async {
            handleArtifacts()
        }

        for (artifact in artifacts) {
            channel.send(DependencyEvent(artifact, null))
        }

        handleArtifacts.join()
    }

    private suspend fun handleArtifacts() {
        for (event in channel) {
            logger.debug { "Received event $event" }
            writeAheadLogService.write(event)
            when (event) {
                is DependencyEvent -> {
                    logger.info { "Handling dependency ${event.artifact}" }
                    val added = dependencyIndex.add(event.artifact, event.parent)
                    if (!added) logger.debug { "Artifact already exists ${event.artifact}" }
                    if (added) {
                        GlobalScope.launch {
                            logger.debug { "Coroutine for resolving dependency started ${event.artifact}" }
                            try {
                                logger.debug { "Resolving dependencies for ${event.artifact}" }
                                val dependencies = modelDependencyResolver.resolveDependencies(event.artifact)
                                for (dependency in dependencies) {
                                    logger.debug { "Sending child into channel $dependency" }
                                    channel.send(DependencyEvent(dependency, event.artifact))
                                }
                                logger.debug { "Sending mark handled event into channel for ${event.artifact}" }
                                channel.send(MarkHandledEvent(event.artifact))
                            } catch (e: Exception) {
                                logger.error { e }
                                channel.close()
                                throw e
                            }
                        }
                    }
                }
                is MarkHandledEvent -> {
                    dependencyIndex.markCompleted(event.artifact)
                    if (dependencyIndex.isAllCompleted()) {
                        channel.close()
                    }
                }
                is RepositoryAddedEvent -> {
                    if (event.replaced) {
                        repositories.firstOrNull { event.repository.url == it.url }?.let {
                            repositories.remove(it)
                            repositories.add(event.repository)
                        }
                    } else {
                        repositories.add(event.repository)
                    }
                }
            }
        }
    }
}