package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.integration.SimpleHttpResolver
import com.github.singleton11.depenencydl.model.*
import com.github.singleton11.depenencydl.persistence.DependencyIndex
import com.github.singleton11.depenencydl.util.InputDigestGenerator
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.maven.model.Repository
import java.io.File

class StateRestoring(private val dependencyIndex: DependencyIndex, val modelResolver: SimpleHttpResolver) {

    private val logger = KotlinLogging.logger { }

    fun restoreState(artifacts: List<Artifact>): Boolean {
        val indexFile = File("index/" + InputDigestGenerator.generate(artifacts))
        logger.info { "Trying to restore state" }
        if (indexFile.exists() && indexFile.length() > 0) {
            logger.info { "WOL found, restoring state" }
            val jsons = indexFile.readLines()
            for (json in jsons) {
                logger.debug { "Handling event $json" }
                when (val event: Event = Json.decodeFromString(json)) {
                    is DependencyEvent -> {
                        dependencyIndex.add(event.artifact, event.parent)
                    }
                    is MarkHandledEvent -> {
                        dependencyIndex.markCompleted(event.artifact)
                    }
                    is RepositoryAddedEvent -> {
                        val repository = Repository()
                        repository.id = event.repository.id
                        repository.url = event.repository.url
                        modelResolver.addRepository(repository, event.replaced)
                    }
                }
            }
            return true
        }
        return false
    }

    fun getDependencyBuilderInput(): List<DependencyEvent> = dependencyIndex.getNotHandled()
}