package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Dependency

class SimpleDependencyConflictResolver : DependencyConflictResolver {
    override fun resolve(dependency1: Dependency, dependency2: Dependency) = dependency2
}