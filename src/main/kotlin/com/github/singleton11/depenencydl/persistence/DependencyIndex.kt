package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact

interface DependencyIndex {
    fun add(artifact: Artifact, parent: Artifact? = null)
    fun markCompleted(artifact: Artifact)
    fun isAllCompleted(): Boolean
    fun getDependenciesToDownload(): List<Artifact>
}