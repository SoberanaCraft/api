package org.siscode.soberanacraft

import io.ktor.server.application.*
import org.siscode.soberanacraft.dao.DatabaseFactory
import org.siscode.soberanacraft.discord.DiscordFactory

fun main(args: Array<String>) : Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    ConfigFactory.init("config.toml")
    DiscordFactory.init(ConfigFactory.config)
    DatabaseFactory.init()
    configureSecurity()
    configureRouting()
    configureSerialization()
}