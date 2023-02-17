package net.soberanacraft.api.discord.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: ULong,
    val username: String,
    val discriminator: String,
    val email: String?
)
