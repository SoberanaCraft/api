package org.siscode.soberanacraft.discord

import mu.KotlinLogging
import org.siscode.soberanacraft.discord.client.DiscordOAuth
import org.siscode.soberanacraft.models.Config

object DiscordFactory {
    private val logger = KotlinLogging.logger {}
    fun init(config: Config) {
        logger.info { "Starting..." }
        DiscordOAuth.init(config)
        logger.info { "Done." }
    }
}