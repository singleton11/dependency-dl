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
data class Dependency(val groupId: String, val artifactId: String, val version: String?)