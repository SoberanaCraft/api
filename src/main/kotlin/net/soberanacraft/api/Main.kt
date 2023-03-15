package net.soberanacraft.api

import io.ktor.server.application.*
import net.soberanacraft.api.dao.DatabaseFactory
import net.soberanacraft.api.discord.DiscordFactory

fun main(args: Array<String>) : Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    ConfigFactory.init("config.toml")
    DiscordFactory.init(ConfigFactory.config)
    DatabaseFactory.init()
    configureSecurity()
    configureRouting()
    configureSerialization()
}
