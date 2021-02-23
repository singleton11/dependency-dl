package com.github.singleton11.depenencydl.util

import com.github.singleton11.depenencydl.model.Artifact
import java.security.MessageDigest

class InputDigestGenerator {
    companion object {
        fun generate(artifacts: List<Artifact>) = MessageDigest
            .getInstance("SHA-256")
            .digest(artifacts.map { it.toString() }.reduce { acc, s -> acc + s }.toByteArray())
            .fold("", { str, it -> str + "%02x".format(it) })
    }
}