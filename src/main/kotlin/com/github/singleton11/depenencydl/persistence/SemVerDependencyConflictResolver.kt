package com.github.singleton11.depenencydl.persistence

import com.github.singleton11.depenencydl.model.Artifact
import com.github.zafarkhaja.semver.Version

class SemVerDependencyConflictResolver : DependencyConflictResolver {
    override fun resolve(artifact1: Artifact, artifact2: Artifact): Artifact {
        try {
            if (Version.valueOf(artifact1.version) <= Version.valueOf(artifact2.version)) {
                return artifact1
            }
            return artifact2
        } catch (e: Exception) {
            if (artifact1.version <= artifact2.version) {
                return artifact1
            }
            return artifact2
        }
    }
}