package com.github.singleton11.depenencydl.persistence.model

import com.github.singleton11.depenencydl.model.Artifact


data class DependencyTreeNode(
    val artifact: Artifact,
    val children: MutableSet<DependencyTreeNode>,
    val parents: Set<DependencyTreeNode>,
    val completed: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DependencyTreeNode

        if (artifact != other.artifact) return false

        return true
    }

    override fun hashCode(): Int {
        return artifact.hashCode()
    }

    override fun toString() =
        "DependencyTreeNode(dependency=$artifact, children=$children, completed=$completed)"

    companion object {
        fun quasiNode() = DependencyTreeNode(
            Artifact.quasiArtifact(),
            mutableSetOf(),
            setOf(),
            false
        )
    }
}
