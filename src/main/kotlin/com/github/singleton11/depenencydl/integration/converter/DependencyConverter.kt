package com.github.singleton11.depenencydl.integration.converter

import com.github.singleton11.depenencydl.integration.model.Dependency

class DependencyConverter {
    fun convert(dependency: Dependency) =
        com.github.singleton11.depenencydl.model.Dependency(
            dependency.groupId,
            dependency.artifactId,
            dependency.version
        )
}