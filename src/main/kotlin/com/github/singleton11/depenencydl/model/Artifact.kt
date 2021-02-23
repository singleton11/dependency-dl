package com.github.singleton11.depenencydl.model

import kotlinx.serialization.Serializable

@Serializable
data class Artifact(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    override fun toString() = "$groupId:$artifactId:$version"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Artifact

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        return result
    }

    companion object {
        fun quasiArtifact() = Artifact("ROOT", "ROOT", "ROOT")
    }
}
