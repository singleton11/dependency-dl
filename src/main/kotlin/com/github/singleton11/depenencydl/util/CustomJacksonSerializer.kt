package com.github.singleton11.depenencydl.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*

class CustomJacksonSerializer(jackson: ObjectMapper = jacksonObjectMapper(), block: ObjectMapper.() -> Unit = {}) :
    JsonSerializer {

    private val backend = jackson.apply(block)

    override fun write(data: Any, contentType: ContentType): OutgoingContent =
        TextContent(backend.writeValueAsString(data), contentType)

    override fun read(type: TypeInfo, body: Input): Any {
        val transformedText = Regex("<!--.*-->")
            .replace(body.readText(), "")
        val transformedText1 = Regex("<dependencies>[\n*\\s]*</dependencies>")
            .replace(transformedText, "<dependencies></dependencies>")
        return backend.readValue(transformedText1, backend.typeFactory.constructType(type.reifiedType))
    }
}