package com.github.singleton11.depenencydl.integration.exception

class DependencyNotFoundException(groupId: String, artifactId: String, version: String) :
    Exception("Dependency $groupId:$artifactId:$version not found")