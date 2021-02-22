package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact

interface DependencyConflictResolver {
    fun resolve(artifact1: Artifact, artifact2: Artifact): Artifact
}