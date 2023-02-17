@file:Suppress("unused")

package net.soberanacraft.api.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.soberanacraft.api.UUIDSerializer
import java.util.UUID

@Serializable
open class Message()

@Serializable
data class LinkMessage(
    @Serializable(with = UUIDSerializer::class) val player: UUID,
    val linkStatus: LinkStatus,
    val discordId: ULong?,
    val joinTime: Instant?
) : Message()

@Serializable
open class ErrorMessage(val error: String) : Message()

@Serializable
data class SucessMessage(val message: String) : Message()

@Serializable
data class InvalidUUIDMessage(val param: String, val value: String): ErrorMessage("Invalid UUID")

@Serializable
enum class LinkStatus {
    InvalidDiscord, NotJoinedToGuild, JoinedGuild, AlreadyLinked
}