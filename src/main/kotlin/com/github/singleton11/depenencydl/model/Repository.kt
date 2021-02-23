package com.github.singleton11.depenencydl.model

import kotlinx.serialization.Serializable

@Serializable
data class Repository(val id: String, val url: String)
