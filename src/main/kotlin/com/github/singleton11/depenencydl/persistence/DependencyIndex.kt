package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.DependencyEvent

interface DependencyIndex {
    fun add(artifact: Artifact, parent: Artifact? = null): Boolean
    fun markCompleted(artifact: Artifact)
    fun isAllCompleted(): Boolean
    fun alreadyHandled(artifact: Artifact): Boolean
    fun getNotHandled(): List<DependencyEvent>
    fun getDependenciesToDownload(): List<Artifact>
}