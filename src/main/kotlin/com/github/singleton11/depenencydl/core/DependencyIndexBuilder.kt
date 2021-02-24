package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.integration.ModelDependencyResolver
import com.github.singleton11.depenencydl.model.*
import com.github.singleton11.depenencydl.persistence.DependencyIndex
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import mu.KotlinLogging

class DependencyIndexBuilder(
    private val dependencyIndex: DependencyIndex,
    val modelDependencyResolver: ModelDependencyResolver,
    manualReplacements: List<Pair<Artifact, Artifact>> = listOf(),

    ) {
    private val logger = KotlinLogging.logger { }
    private val channel: Channel<Event> = Channel(Channel.UNLIMITED)
    private val internalManualReplacements: Map<Triple<String, String, String>, Triple<String, String, String>> =
        manualReplacements
            .map {
                Triple(it.first.groupId, it.first.artifactId, it.first.version) to Triple(
                    it.second.groupId,
                    it.second.artifactId,
                    it.second.version
                )
            }
            .toMap()

    suspend fun build(artifacts: List<Artifact>) {

        val handleArtifacts = GlobalScope.async {
            handleArtifacts()
        }

        for (artifact in artifacts) {
            channel.send(DependencyEvent(artifact, null))
        }

        handleArtifacts.join()
    }

    fun isBuildCompleted() = dependencyIndex.isAllCompleted()

    fun getDependenciesForDownload() = dependencyIndex.getDependenciesToDownload()

    private suspend fun handleArtifacts() {
        for (event in channel) {
            logger.debug { "Received event $event" }
            when (event) {
                is DependencyEvent -> {
                    logger.info { "Handling dependency ${event.artifact}" }
                    dependencyIndex.add(event.artifact, event.parent)
                    GlobalScope.launch {
                        logger.debug { "Coroutine for resolving dependency started ${event.artifact}" }
                        try {
                            logger.debug { "Resolving dependencies for ${event.artifact}" }
                            val dependencies = modelDependencyResolver.resolveDependencies(event.artifact)
                            for (dependency in dependencies) {
                                val newDependency = replaceManualDependencyIfNeeded(dependency)
                                logger.debug { "Sending child into channel $newDependency" }
                                channel.send(DependencyEvent(newDependency, event.artifact))
                            }
                            logger.debug { "Sending mark handled event into channel for ${event.artifact}" }
                            channel.send(MarkHandledEvent(event.artifact))
                        } catch (e: ClosedSendChannelException) {
                            // Do nothing
                        } catch (e: Exception) {
                            logger.error { e }
                            channel.close()
                            throw e
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

    private fun replaceManualDependencyIfNeeded(dependency: Artifact): Artifact {
        val (groupId, artifactId, version) = dependency
        val (newGroupId, newArtifactId, newVersion) = internalManualReplacements[Triple(groupId, artifactId, version)]
            ?: Triple(groupId, artifactId, version)
        return Artifact(newGroupId, newArtifactId, newVersion)
    }
}