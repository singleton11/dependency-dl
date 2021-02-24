package com.github.singleton11.depenencydl.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Event

@Serializable
data class DependencyEvent(val artifact: Artifact, val parent: Artifact?) : Event()

@Serializable
data class MarkHandledEvent(val artifact: Artifact) : Event()
