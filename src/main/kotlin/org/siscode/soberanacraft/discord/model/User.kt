package org.siscode.soberanacraft.discord.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: ULong,
    val username: String,
    val discriminator: String,
    val email: String?
)
