package org.siscode.soberanacraft.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.siscode.soberanacraft.UUIDSerializer
import java.util.UUID

object Servers : UUIDTable() {
    val name = varchar("name", 32)
    val playerCount = integer("playerCount").default(0)
    val supportedVersions = text("supportedVersions").default("[]")
    val internalIp = text("internalIp").nullable()
    val externalIp = text("externalIp")
    val modded = bool("modded").default(false)
    val platform = enumerationByName<Platform>("platform", 10).default(Platform.Java)
    val modpackUrl = text("modpackUrl").nullable()
    val modloader = text("modloader").nullable()
}

@Serializable
data class ServerStub(
    val name: String,
    val supportedVersions: String,
    val externalIp: String,
    val internalIp: String?,
    val modded: Boolean,
    val platform: Platform,
    val modpackUrl: String?,
    val modloader: String?
)

@Serializable
data class Server(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val playerCount: Int,
    val supportedVersions: String,
    val externalIp: String,
    val internalIp: String?,
    val modded: Boolean,
    val platform: Platform,
    val modpackUrl: String?,
    val modloader: String?
)


fun ResultRow.intoServer() = Server(
    id = this[Servers.id].value,
    name = this[Servers.name],
    playerCount = this[Servers.playerCount],
    supportedVersions = this[Servers.supportedVersions],
    externalIp = this[Servers.externalIp],
    internalIp = this[Servers.internalIp],
    modded = this[Servers.modded],
    platform = this[Servers.platform],
    modpackUrl = this[Servers.modpackUrl],
    modloader = this[Servers.modloader]
)