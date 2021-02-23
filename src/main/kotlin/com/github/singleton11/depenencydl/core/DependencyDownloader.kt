package com.github.singleton11.depenencydl.core

import com.github.singleton11.depenencydl.core.exception.BuildException
import com.github.singleton11.depenencydl.model.Artifact
import mu.KotlinLogging

class DependencyDownloader(
    private val stateRestoring: StateRestoring,
    private val dependencyIndexBuilder: DependencyIndexBuilder
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

        println("We need to download following dependencies")
        println(dependencyIndexBuilder.getDependenciesForDownload())
    }
}