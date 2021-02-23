package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.core.exception.BuildException
import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Repository
import com.github.singleton11.depenencydl.persistence.jar.JarDownloader
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import mu.KotlinLogging

class DependencyDownloader(
    private val stateRestoring: StateRestoring,
    private val dependencyIndexBuilder: DependencyIndexBuilder,
    private val jarDownloader: JarDownloader
) {

    private val logger = KotlinLogging.logger { }

    suspend fun run(artifacts: List<Artifact>) {
        if (stateRestoring.restoreState(artifacts)) {
            if (!dependencyIndexBuilder.isBuildCompleted()) {
                dependencyIndexBuilder.buildRestored(stateRestoring.getDependencyBuilderInput())
            }
        } else {
            dependencyIndexBuilder.build(artifacts)
        }

        if (!dependencyIndexBuilder.isBuildCompleted()) {
            throw BuildException()
        }

        downloadJars()
    }

    private suspend fun downloadJars() {
        logger.info { "Downloading jars" }
        val dependenciesForDownload = dependencyIndexBuilder.getDependenciesForDownload()
        val repositories = stateRestoring.modelResolver.repositories.map { Repository(it.id, it.url) }
        val coroutines = mutableListOf<Deferred<Unit>>()
        for (artifact in dependenciesForDownload) {
            val deferred = GlobalScope.async {
                jarDownloader.download(artifact, repositories)
            }
            coroutines.add(deferred)
        }
        coroutines.awaitAll()
    }
}