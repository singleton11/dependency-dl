package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.core.model.DependencyEvent
import com.github.singleton11.depenencydl.core.model.Event
import com.github.singleton11.depenencydl.core.model.MarkHandledEvent
import com.github.singleton11.depenencydl.integration.ModelDependencyResolver
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.persistence.DependencyIndex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KotlinLogging

class DependencyIndexBuilder(
    private val dependencyIndex: DependencyIndex,
    private val modelDependencyResolver: ModelDependencyResolver
) {
    private val logger = KotlinLogging.logger { }

    suspend fun build(artifacts: List<Artifact>) {
        val channel: Channel<Event> = Channel()

        val handleArtifacts = GlobalScope.async {
            handleArtifacts(channel)
        }

        for (artifact in artifacts) {
            channel.send(DependencyEvent(artifact, null))
        }

        handleArtifacts.join()
    }

    private suspend fun handleArtifacts(channel: Channel<Event>) {
        for (event in channel) {
            when (event) {
                is DependencyEvent -> {
                    logger.info { "Handling dependency ${event.artifact}" }

                    val added = dependencyIndex.add(event.artifact, event.parent)
                    if (added) {
                        GlobalScope.launch {
                            try {
                                val dependencies = modelDependencyResolver.resolveDependencies(event.artifact)
                                for (dependency in dependencies) {
                                    channel.send(DependencyEvent(dependency, event.artifact))
                                }
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
            }
        }

    }
}