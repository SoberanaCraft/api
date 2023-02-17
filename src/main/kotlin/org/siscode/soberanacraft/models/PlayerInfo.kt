package org.siscode.soberanacraft.models

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.siscode.soberanacraft.UUIDSerializer
import java.util.UUID


object Players : Table() {
    val uuid = uuid("uuid")
    val nickname = varchar("nickname", 48)
    val platform = enumerationByName<Platform>("platform", 10)
    val discordId = ulong("discordId").nullable()
    val joinedAt = timestamp("joinedAt")
    val linkedAt = timestamp("linkedAt").nullable()
    val trustFactor = enumerationByName<Trust>("trustFactor", 16).default(Trust.Unlinked)
    val referer = uuid("referer").nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(uuid)
}

@Serializable
data class PlayerStub(@Serializable(with = UUIDSerializer::class) val uuid: UUID,
                      val nickname: String,
                      val platform: Platform)

@Serializable
data class Player(
    @Serializable(with = UUIDSerializer::class) val uuid: UUID,
    val nickname: String,
    val platform: Platform,
    val discordId: ULong?,
    val joinedAt: Instant,
    val linkedAt: Instant?,
    val trustFactor: Trust,
    @Serializable(with = UUIDSerializer::class) val referer: UUID?
)

@Suppress("unused")
@Serializable
enum class Platform {
    Bedrock, Java, Both
}

@Serializable
enum class Trust {
    Unlinked, Linked, Reffered, Trusted
}

fun ResultRow.intoPlayer() = Player(
    uuid = this[Players.uuid],
    nickname = this[Players.nickname],
    platform = this[Players.platform],
    discordId = this[Players.discordId],
    joinedAt = this[Players.joinedAt].toKotlinInstant(),
    linkedAt = this[Players.linkedAt]?.toKotlinInstant(),
    trustFactor = this[Players.trustFactor],
    referer = this[Players.referer]
)