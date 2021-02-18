package com.github.singleton11.depenencydl.model

data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dependency

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        return result
    }

    override fun toString() = "$groupId:$artifactId:$version"

    companion object {
        fun quasiDependency() = Dependency("ROOT", "ROOT", "ROOT")
    }
}
