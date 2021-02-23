package com.github.singleton11.depenencydl.persistence.wol

import com.github.singleton11.depenencydl.model.Artifact
import com.github.singleton11.depenencydl.model.Event
import com.github.singleton11.depenencydl.util.InputDigestGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream

class WriteAheadLogService(artifacts: List<Artifact>) {

    private val file: File
    private val logger = KotlinLogging.logger { }

    init {
        File("index/").mkdirs()
        file = File("index/" + InputDigestGenerator.generate(artifacts))
    }

    fun write(event: Event) {
        logger.debug { "Write ahead $event" }
        val encoded = Json.encodeToString(event)
        FileOutputStream(file, true)
            .bufferedWriter()
            .use { writer ->
                writer.write("$encoded\n")
            }
        logger.debug { "Write ahead finished for $event" }
    }
}