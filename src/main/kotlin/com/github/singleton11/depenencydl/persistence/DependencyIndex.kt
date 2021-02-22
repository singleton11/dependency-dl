package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact

interface DependencyIndex {
    fun add(artifact: Artifact, parent: Artifact? = null): Boolean
    fun markCompleted(artifact: Artifact)
    fun isAllCompleted(): Boolean
    fun alreadyHandled(artifact: Artifact): Boolean
}