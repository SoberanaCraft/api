package net.soberanacraft.api.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import net.soberanacraft.api.discord.client.DiscordOAuth
import net.soberanacraft.api.discord.model.TokensResponse
import net.soberanacraft.api.discord.model.User
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

object Discord : Table() {
    val id = ulong("id")
    val username = text("username")
    val discriminator = integer("discriminator")
    val nickname = text("nickname").nullable()
    val email = text("email")
    val joinedAt = timestamp("joinedAt")
    val accessToken = text("accessToken")
    val refreshToken = text("refreshToken")
    val expiresAt = timestamp("expiresAt")

    override val primaryKey = PrimaryKey(id)
}

data class DiscordUser(
    val id: ULong,
    val username: String,
    val discriminator: Int,
    val nickname: String?,
    val email: String,
    val joinedAt: Instant,
    var accessToken: String,
    var refreshToken: String,
    var expiresAt: Instant
) {
    override fun toString(): String = "$username#$discriminator ($id)"
}

fun ResultRow.intoDiscordUser() = DiscordUser(
    id = this[Discord.id],
    username = this[Discord.username],
    discriminator = this[Discord.discriminator],
    nickname = this[Discord.nickname],
    email = this[Discord.email],
    joinedAt = this[Discord.joinedAt].toKotlinInstant(),
    accessToken = this[Discord.accessToken],
    refreshToken = this[Discord.refreshToken],
    expiresAt = this[Discord.expiresAt].toKotlinInstant()
)

fun User.into(joinedAt: Instant, nickname: String?, tokensResponse: TokensResponse) = DiscordUser(
    id = this.id,
    username = this.username,
    discriminator = this.discriminator.toInt(),
    nickname = nickname,
    accessToken = tokensResponse.accessToken,
    refreshToken = tokensResponse.refreshToken,
    email = this.email!!,
    joinedAt = joinedAt,
    expiresAt = Clock.System.now() + (tokensResponse.expiresIn - 10).seconds
)

fun DiscordUser.trust(): Trust {
    return if (Clock.System.now() - joinedAt >= 7.days) {
        Trust.Trusted
    } else {
        Trust.Linked
    }
}

fun DiscordUser.expired(): Boolean =
    Clock.System.now() >= this.expiresAt

suspend fun DiscordUser.refresh(): Message {
    val tokens = DiscordOAuth.refreshTokens(this.refreshToken) ?: return ErrorMessage("Invalid refresh token for $this")
    this.accessToken = tokens.accessToken
    this.refreshToken = tokens.refreshToken
    this.expiresAt = Clock.System.now() + (tokens.expiresIn - 10).seconds
    return SucessMessage("Refreshed tokens for $this")
}