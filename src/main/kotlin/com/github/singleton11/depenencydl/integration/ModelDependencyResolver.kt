package com.github.singleton11.depenencydl.integration

import com.github.singleton11.depenencydl.model.Artifact
import org.apache.maven.artifact.versioning.VersionRange
import org.apache.maven.model.Dependency
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.resolution.ModelResolver

class ModelDependencyResolver(private val modelBuilder: ModelBuilder, val modelResolver: ModelResolver) {
    fun resolveDependencies(artifact: Artifact): List<Artifact> {
        val resolvedModel = modelResolver
            .resolveModel(artifact.groupId, artifact.artifactId, artifact.version) as FileModelSource
        val request = DefaultModelBuildingRequest()
        request.validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL
        request.modelResolver = modelResolver
        request.pomFile = resolvedModel.file
        request.systemProperties["java.version"] = "11"
        request.systemProperties["java.home"] = "/"
        request.systemProperties["java.specification.version"] = "11"
        request.systemProperties["os.arch"] = "amd64"
        val effectiveModel = modelBuilder.build(request).effectiveModel
        return effectiveModel
            .dependencies
            .filter { it.scope != "system" }
            .filter { !it.isOptional }
            .filter { it.scope != "provided" }
            .map { dependency ->
                resolveDependencyVersionRange(dependency)
            }
    }

    private fun resolveDependencyVersionRange(dependency: Dependency): Artifact {
        val createFromVersionSpec = VersionRange.createFromVersionSpec(dependency.version)
        val dependencyVersion = createFromVersionSpec.recommendedVersion?.toString()
            ?: handleNoRecommendedVersion(createFromVersionSpec)
            ?: dependency.version
        return Artifact(dependency.groupId, dependency.artifactId, dependencyVersion)
    }

    private fun handleNoRecommendedVersion(createFromVersionSpec: VersionRange) =
        getLowerBound(createFromVersionSpec)
            ?: getUpperBound(createFromVersionSpec)

    private fun getUpperBound(createFromVersionSpec: VersionRange) =
        createFromVersionSpec
            .restrictions
            .firstOrNull { restriction -> restriction.upperBound != null && restriction.isUpperBoundInclusive }
            ?.upperBound
            ?.toString()

    private fun getLowerBound(createFromVersionSpec: VersionRange) =
        createFromVersionSpec
            .restrictions
            .firstOrNull { restriction -> restriction.lowerBound != null && restriction.isLowerBoundInclusive }
            ?.lowerBound
            ?.toString()
}