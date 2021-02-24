package com.github.singleton11.depenencydl.core.exception

import com.github.singleton11.depenencydl.model.Artifact


class DependencyJarNotFoundException(artifact: Artifact) : Exception("Dependency jar not found for artifact $artifact")