package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.persistence.model.DependencyTreeNode
import java.util.*

class TreeDependencyIndex(private val dependencyConflictResolver: DependencyConflictResolver) : DependencyIndex {

    private val rootNode: DependencyTreeNode = DependencyTreeNode.quasiNode()
    private val dependencyMap: MutableMap<Artifact, DependencyTreeNode> = mutableMapOf()
    private val handledDependencies: MutableSet<Triple<String, String, String>> = mutableSetOf()
    private val notHandledDependencies: MutableSet<Triple<String, String, String>> = mutableSetOf()

    override fun add(artifact: Artifact, parent: Artifact?) = parent?.let {
        if (!handledDependencies.contains(Triple(artifact.groupId, artifact.artifactId, artifact.version))) {
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

    override fun isAllCompleted() = notHandledDependencies.isEmpty()

    override fun getDependenciesToDownload(): List<Artifact> {
        val stack = Stack<DependencyTreeNode>()
        stack.push(rootNode)

        val artifacts = mutableListOf<Artifact>()

        val markedNodes = mutableSetOf<DependencyTreeNode>()

        while (stack.isNotEmpty()) {
            val dependencyTreeNode = stack.pop()
            markedNodes.add(dependencyTreeNode)
            artifacts.add(dependencyTreeNode.artifact)
            stack.addAll(dependencyTreeNode.children.filter { !markedNodes.contains(it) })
        }

        return artifacts.filter { it != Artifact.quasiArtifact() }
    }

    private fun add(artifact: Artifact, parentNode: DependencyTreeNode) {
        return dependencyMap[artifact]?.let { existedNode ->
            dependencyMap[artifact] = addAlreadyExistsDependency(artifact, parentNode, existedNode)
        } ?: kotlin.run {
            dependencyMap[artifact] = addNewDependency(artifact, parentNode)
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