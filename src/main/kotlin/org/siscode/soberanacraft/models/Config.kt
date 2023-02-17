package org.siscode.soberanacraft.models

import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.file.TomlFileWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class Config(val clientId: String, val clientSecret: String, val redirectUri: String, val scope: List<String>, val bearer: String){
    companion object {
        val DEFAULT = Config("", "", "", listOf(), "")
    }
}

fun new(path: String) {
    TomlFileWriter().encodeToFile(Config.serializer(), Config.DEFAULT, path)
}

fun from(path: String): Config =
    TomlFileReader.decodeFromFile(serializer(), path)
