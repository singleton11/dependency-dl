package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Dependency
import com.github.singleton11.depenencydl.persistence.model.DependencyTreeNode

class DependencyIndex(private val dependencyConflictResolver: DependencyConflictResolver) {

    private val rootNode: DependencyTreeNode = DependencyTreeNode.quasiNode()
    private val dependencyMap: MutableMap<Dependency, DependencyTreeNode> = mutableMapOf()

    var completed: Boolean = false

    fun add(dependency: Dependency, parent: Dependency? = null) = parent?.let {
        dependencyMap[parent]?.let { parentNode ->
            add(dependency, parentNode)
        }
    } ?: kotlin.run {
        add(dependency, rootNode)
    }

    fun markCompleted(dependency: Dependency) {
        dependencyMap[dependency]?.let {
            replaceDependencyTreeNode(it.copy(completed = true), it)
        }
    }

    private fun add(dependency: Dependency, parentNode: DependencyTreeNode) {
        val newNode = dependencyMap[dependency]?.let { existedNode ->
            addAlreadyExistsDependency(dependency, parentNode, existedNode)
        } ?: kotlin.run {
            addNewDependency(dependency, parentNode)
        }
        dependencyMap[dependency] = newNode
    }

    private fun addAlreadyExistsDependency(
        dependency: Dependency,
        parentNode: DependencyTreeNode,
        existedNode: DependencyTreeNode
    ): DependencyTreeNode {
        // resolve conflict
        val existedDependency = existedNode.dependency
        val newDependency = dependencyConflictResolver.resolve(existedDependency, dependency)

        // replace dependency
        val newDependencyTreeNode = existedNode.copy(
            dependency = newDependency,
            parents = existedNode.parents + parentNode
        )
        return replaceDependencyTreeNode(newDependencyTreeNode, existedNode)
    }

    private fun addNewDependency(
        dependency: Dependency,
        parentDependencyTreeNode: DependencyTreeNode
    ): DependencyTreeNode {
        val newDependencyTreeNode = DependencyTreeNode(
            dependency,
            mutableSetOf(),
            setOf(parentDependencyTreeNode),
            false
        )
        parentDependencyTreeNode.children.add(newDependencyTreeNode)
        return newDependencyTreeNode
    }

    private fun replaceDependencyTreeNode(
        new: DependencyTreeNode,
        old: DependencyTreeNode
    ): DependencyTreeNode {
        new.parents.forEach {
            with(it.children) {
                this.remove(old)
                this.add(new)
            }
        }

        return new
    }
}