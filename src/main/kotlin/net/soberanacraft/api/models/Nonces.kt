package net.soberanacraft.api.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import net.soberanacraft.api.UUIDSerializer
import java.util.*

object Nonces : Table() {
    val nonce = varchar("nonce", 32)
    val uuid = uuid("uuid")

    override val primaryKey: PrimaryKey = PrimaryKey(nonce)
}

@Serializable
data class Nonce(@Serializable(with = UUIDSerializer::class) val playerId: UUID, val nonce: String)

fun ResultRow.intoNonce() = Nonce(
    playerId = this[Nonces.uuid],
    nonce = this[Nonces.nonce]
)