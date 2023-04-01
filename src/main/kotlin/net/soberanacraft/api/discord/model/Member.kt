package net.soberanacraft.api.discord.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val user: User,
    @SerialName("joined_at") val joinedAt: Instant,
    @SerialName("nick") val nickname: String?
)