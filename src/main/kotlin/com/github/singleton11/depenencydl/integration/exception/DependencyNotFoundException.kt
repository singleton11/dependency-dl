package com.github.singleton11.depenencydl.integration.exception

import com.github.singleton11.depenencydl.model.Dependency

class DependencyNotFoundException(dependency: Dependency) : Exception("Dependency $dependency not found")