package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.persistence.model.DependencyTreeNode

class TreeDependencyIndex(private val dependencyConflictResolver: DependencyConflictResolver) : DependencyIndex {

    private val rootNode: DependencyTreeNode = DependencyTreeNode.quasiNode()
    private val dependencyMap: MutableMap<Artifact, DependencyTreeNode> = mutableMapOf()
    private val handledDependencies: MutableSet<Triple<String, String, String>> = mutableSetOf()
    private val notHandledDependencies: MutableSet<Triple<String, String, String>> = mutableSetOf()

    override fun add(artifact: Artifact, parent: Artifact?) = parent?.let {
        if (handledDependencies.contains(Triple(artifact.groupId, artifact.artifactId, artifact.version))) {
            false
        } else {
            handledDependencies.add(Triple(artifact.groupId, artifact.artifactId, artifact.version))
            notHandledDependencies.add(Triple(artifact.groupId, artifact.artifactId, artifact.version))
            dependencyMap[parent]?.let { parentNode ->
                add(artifact, parentNode)
            }
        }
    } ?: kotlin.run {
        add(artifact, rootNode)
    }

    override fun markCompleted(artifact: Artifact) {
        notHandledDependencies.remove(Triple(artifact.groupId, artifact.artifactId, artifact.version))
    }

    override fun alreadyHandled(artifact: Artifact) =
        handledDependencies.contains(Triple(artifact.groupId, artifact.artifactId, artifact.version))

    override fun isAllCompleted() = notHandledDependencies.isEmpty()

    private fun add(artifact: Artifact, parentNode: DependencyTreeNode): Boolean {
        return dependencyMap[artifact]?.let { existedNode ->
            val newNode = addAlreadyExistsDependency(artifact, parentNode, existedNode)
            dependencyMap[artifact] = newNode
            newNode.artifact.version != existedNode.artifact.version
        } ?: kotlin.run {
            dependencyMap[artifact] = addNewDependency(artifact, parentNode)
            true
        }

    }

    private fun addAlreadyExistsDependency(
        artifact: Artifact,
        parentNode: DependencyTreeNode,
        existedNode: DependencyTreeNode
    ): DependencyTreeNode {
        // resolve conflict
        val existedDependency = existedNode.artifact
        val newDependency = dependencyConflictResolver.resolve(existedDependency, artifact)

        // replace dependency
        val newDependencyTreeNode = existedNode.copy(
            artifact = newDependency,
            parents = existedNode.parents + parentNode
        )
        return replaceDependencyTreeNode(newDependencyTreeNode, existedNode)
    }

    private fun addNewDependency(
        dependency: Artifact,
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