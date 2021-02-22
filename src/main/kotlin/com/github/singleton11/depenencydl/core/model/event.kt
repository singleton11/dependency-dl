package com.github.singleton11.depenencydl.core.model

import com.github.singleton11.depenencydl.model.Artifact

sealed class Event

data class DependencyEvent(val artifact: Artifact, val parent: Artifact?) : Event()

data class MarkHandledEvent(val artifact: Artifact) : Event()
