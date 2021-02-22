package com.github.singleton11.depenencydl.integration

import org.apache.maven.model.building.ModelSource2
import java.io.File
import java.io.InputStream
import java.net.URI

/**
 * A local implementation of an internal part of the resolving infrastructure. This simply wraps
 * the locally downloaded file and gives its information to the maven model infrastructure.
 */
data class FileModelSource(val file: File) : ModelSource2 {
    override fun getLocationURI(): URI = file.toURI()

    override fun getLocation(): String = file.toPath().toString()

    override fun getRelatedSource(relativePath: String) = null // We don't handle local project layouts

    override fun getInputStream(): InputStream = file.inputStream()
}