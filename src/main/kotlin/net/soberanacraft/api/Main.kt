package net.soberanacraft.api

import io.ktor.serialization.kotlinx.*
import io.ktor.server.websocket.*
import io.ktor.server.application.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import net.soberanacraft.api.dao.DatabaseFactory
import net.soberanacraft.api.discord.DiscordFactory
import net.soberanacraft.api.models.LinkMessage

fun main(args: Array<String>) : Unit = io.ktor.server.netty.EngineMain.main(args)

object Flows {
    val LinkMessage = MutableSharedFlow<LinkMessage>()
}

fun Application.module() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    val path = System.getenv("CONFIG_PATH") ?: "config/config.toml"
    
    ConfigFactory.init(path)
    DiscordFactory.init(ConfigFactory.config)
    DatabaseFactory.init()
    configureSecurity()
    configureRouting()
    configureSerialization()
}
