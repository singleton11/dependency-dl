package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Dependency

interface DependencyConflictResolver {
    fun resolve(dependency1: Dependency, dependency2: Dependency): Dependency
}