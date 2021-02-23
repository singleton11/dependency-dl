package com.github.singleton11.depenencydl.integration

import org.apache.maven.model.Model
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.building.ModelProblemCollector
import org.apache.maven.model.validation.ModelValidator

class NoopModelValidator : ModelValidator {
    override fun validateRawModel(model: Model?, request: ModelBuildingRequest?, problems: ModelProblemCollector?) {
        // Do nothing
    }

    override fun validateEffectiveModel(
        model: Model?,
        request: ModelBuildingRequest?,
        problems: ModelProblemCollector?
    ) {
        // Do nothing
    }
}