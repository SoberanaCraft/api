package net.soberanacraft.api.discord

import mu.KotlinLogging
import net.soberanacraft.api.discord.client.DiscordOAuth
import net.soberanacraft.api.models.Config

object DiscordFactory {
    private val logger = KotlinLogging.logger {}
    fun init(config: Config) {
        logger.info { "Starting..." }
        DiscordOAuth.init(config)
        logger.info { "Done." }
    }
}