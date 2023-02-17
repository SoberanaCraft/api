package org.siscode.soberanacraft.discord.model

import kotlinx.serialization.Serializable

@Serializable
data class PartialGuild(
    val id: ULong,
    val name: String,
    val icon: String,
    val owner: Boolean,
    val permissions: Long,
    val features: List<String>
)
