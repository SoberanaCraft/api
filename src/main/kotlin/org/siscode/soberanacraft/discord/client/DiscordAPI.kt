package org.siscode.soberanacraft.discord.client

import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging
import org.siscode.soberanacraft.discord.model.Member
import org.siscode.soberanacraft.discord.model.PartialGuild
import org.siscode.soberanacraft.discord.model.User

class DiscordAPI(private val accessToken: String) {
    private val logger = KotlinLogging.logger {  }
    private suspend inline fun <reified T> get(path: String, accessToken: String = this.accessToken): T? {
        logger.trace { "[GET] $BASE_URI$path" }
        val response = DiscordOAuth.client.get (BASE_URI + path) {
            this.bearerAuth(accessToken)
            this.userAgent("SoberanaCraft-API Discord OAuth2 Client")
        }
        logger.trace { "Response: $response" }

        return response.nullableBody()
    }

    suspend fun user() = get<User>("/users/@me")
    suspend fun guilds() = get<List<PartialGuild>>("/users/@me/guilds")
    suspend fun member(guildId: ULong) = get<Member>("/users/@me/guilds/$guildId/member")

    companion object {
        val BASE_URI = "https://discord.com/api"
    }
}