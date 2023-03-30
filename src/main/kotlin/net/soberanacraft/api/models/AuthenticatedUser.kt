package net.soberanacraft.api.models

import kotlinx.serialization.Serializable
import net.soberanacraft.api.UUIDSerializer
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import java.util.UUID

@Serializable
data class AuthenticatedUser(@Serializable(with=UUIDSerializer::class) val owner: UUID, val discordId: ULong?, val password: String)

object Authentication: Table () {
    val owner = uuid("owner")
    val discordId = ulong("discordId").nullable()
    val password = text("password")

    override val primaryKey = PrimaryKey(owner)
}

fun ResultRow.intoAuthenticatedUser() = AuthenticatedUser(
    owner = this[Authentication.owner],
    discordId = this[Authentication.discordId],
    password = this[Authentication.password]
)