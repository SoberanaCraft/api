package net.soberanacraft.api.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import net.soberanacraft.api.UUIDSerializer
import java.util.UUID

object Connections : Table() {
    val player = uuid("player")
    val version = varchar("version", 10)
    val server = reference("server", Servers)

    override val primaryKey: PrimaryKey = PrimaryKey(player)
}

@Serializable
data class Connection(
    @Serializable(with = UUIDSerializer::class) val playerUUID: UUID,
    val version: String,
    @Serializable(with = UUIDSerializer::class) val serverId: UUID
)

fun ResultRow.intoConnection() = Connection(
    playerUUID = this[Connections.player],
    version = this[Connections.version],
    serverId = this[Connections.server].value
)
