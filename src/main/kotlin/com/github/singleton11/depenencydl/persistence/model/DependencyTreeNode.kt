package com.github.singleton11.depenencydl.persistence.model

import com.github.singleton11.depenencydl.model.Dependency

data class DependencyTreeNode(
    val dependency: Dependency,
    val children: MutableSet<DependencyTreeNode>,
    val parents: Set<DependencyTreeNode>,
    val completed: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DependencyTreeNode

        if (dependency != other.dependency) return false

        return true
    }

    override fun hashCode(): Int {
        return dependency.hashCode()
    }

    override fun toString() =
        "DependencyTreeNode(dependency=$dependency, children=$children, completed=$completed)"

    companion object {
        fun quasiNode() = DependencyTreeNode(
            Dependency.quasiDependency(),
            mutableSetOf(),
            setOf(),
            false
        )
    }
}