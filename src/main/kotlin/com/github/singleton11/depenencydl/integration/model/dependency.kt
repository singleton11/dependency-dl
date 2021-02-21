package com.github.singleton11.depenencydl.integration.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
    val dependencies: List<Dependency>?,
    val properties: Map<String, String>?,
    val parent: Dependency?,
    val dependencyManagement: DependencyManagement?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DependencyManagement(val dependencies: List<Dependency>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dependency(val groupId: String, val artifactId: String, val version: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dependency

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        return result
    }
}