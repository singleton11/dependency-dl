package com.github.singleton11.depenencydl.integration.exception

import com.github.singleton11.depenencydl.integration.model.Dependency

class DependencyVersionCantBeResolvedException(dependency: Dependency) :
    Exception("Dependency version can't be resolved $dependency")