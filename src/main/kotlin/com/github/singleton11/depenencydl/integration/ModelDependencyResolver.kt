package com.github.singleton11.depenencydl.integration

import com.github.singleton11.depenencydl.model.Artifact
import org.apache.maven.artifact.versioning.VersionRange
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.resolution.ModelResolver

class ModelDependencyResolver(private val modelBuilder: ModelBuilder, private val modelResolver: ModelResolver) {
    fun resolveDependencies(artifact: Artifact): List<Artifact> {
        val resolvedModel = modelResolver
            .resolveModel(artifact.groupId, artifact.artifactId, artifact.version) as FileModelSource
        val request = DefaultModelBuildingRequest()
        request.validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL
        request.modelResolver = modelResolver
        request.pomFile = resolvedModel.file
        request.systemProperties["java.version"] = "11"
        request.systemProperties["java.home"] = "/"
        return modelBuilder
            .build(request)
            .effectiveModel
            .dependencies
            .filter { it.scope != "system" }
            .map { dependency ->
                val createFromVersionSpec = VersionRange.createFromVersionSpec(dependency.version)
                val dependencyVersion = createFromVersionSpec.recommendedVersion?.toString() ?: kotlin.run {
                    createFromVersionSpec.restrictions.firstOrNull { restriction -> restriction.lowerBound != null && restriction.isLowerBoundInclusive }?.lowerBound?.toString()
                        ?: createFromVersionSpec.restrictions.firstOrNull { restriction -> restriction.upperBound != null && restriction.isUpperBoundInclusive }?.upperBound?.toString()
                } ?: dependency.version
                Artifact(dependency.groupId, dependency.artifactId, dependencyVersion)
            }
    }
}