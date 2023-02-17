package org.siscode.soberanacraft.dao

import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.siscode.soberanacraft.dao.DatabaseFactory.dbQuery
import org.siscode.soberanacraft.models.*
import java.util.*

class DaoFacadeImpl : DaoFacade {
    val logger = KotlinLogging.logger {  }

    override suspend fun allPlayers(): List<Player> = dbQuery {
        logger.trace { "allPlayers" }
        Players.selectAll().map { it.intoPlayer() }
    }

    override suspend fun player(uuid: UUID): Player? = dbQuery {
        logger.trace { "player[$uuid]" }
        Players.select { Players.uuid eq uuid }
            .map { it.intoPlayer() }
            .singleOrNull()
    }

    override suspend fun playerByNickname(nickname: String): Player? = dbQuery {
        logger.trace { "playerByNickname[$nickname]" }
        Players.select { Players.nickname eq nickname }
            .map { it.intoPlayer() }
            .singleOrNull()
    }

    override suspend fun addNewPlayer(uuid: UUID, nickname: String, platform: Platform): Player? = dbQuery {
        logger.trace { "addNewPlayer[$uuid, $nickname, $platform]" }
        val insertStatement = Players.insert {
            it[Players.uuid] = uuid
            it[Players.nickname] = nickname
            it[Players.platform] = platform
            it[linkedAt] = null
            it[discordId] = null
            it[joinedAt] = Clock.System.now().toJavaInstant()
            it[referer] = null
            it[trustFactor] = Trust.Unlinked
        }

        insertStatement.resultedValues?.singleOrNull()?.intoPlayer()
    }

    override suspend fun link(uuid: UUID, discordId: ULong, trust: Trust): Boolean = dbQuery {
        logger.trace { "link[$uuid, $discordId, $trust]" }
        Players.update({ Players.uuid eq uuid }) {
            it[linkedAt] = Clock.System.now().toJavaInstant()
            it[Players.discordId] = discordId
            it[trustFactor] = trust
        } > 0
    }

    override suspend fun unlink(uuid: UUID): Boolean = dbQuery {
        logger.trace { "unlink[$uuid]" }
        Players.update({ Players.uuid eq uuid }) {
            it[linkedAt] = null
            it[discordId] = null
            it[trustFactor] = Trust.Unlinked
        } > 0
    }

    override suspend fun refer(uuid: UUID, referee: UUID): Boolean = dbQuery {
        logger.trace { "refer[$uuid, $referee]" }
        Players.update({ Players.uuid eq uuid }) {
            it[referer] = referee
        } > 0
    }

    override suspend fun deletePlayer(uuid: UUID): Boolean = dbQuery {
        logger.trace { "deletePlayer[$uuid]" }
        Players.deleteWhere { Players.uuid eq uuid } > 0
    }

    override suspend fun allServers(): List<Server> = dbQuery {
        logger.trace { "allServers" }
        Servers.selectAll().map { it.intoServer() }
    }

    override suspend fun server(id: UUID): Server? = dbQuery {
        logger.trace { "server[$id]" }
        Servers.select { Servers.id eq id }
            .map { it.intoServer() }
            .singleOrNull()
    }

    override suspend fun serverByName(name: String): Server? = dbQuery {
        logger.trace { "serverByName[$name]" }
        Servers.select { Servers.name eq name }
            .map { it.intoServer() }
            .singleOrNull()
    }

    override suspend fun updatePlayerCount(id: UUID, count: Int): Boolean = dbQuery {
        logger.trace { "updatePlayerCount[$id, $count]" }
        Servers.update({ Servers.id eq id }) {
            it[playerCount] = count
        } > 0
    }

    override suspend fun updateServer(id: UUID, stub: ServerStub): Boolean = dbQuery {
        logger.trace { "updateServer[$id, ...]" }
        Servers.update({ Servers.id eq id }) {
            it[name] = stub.name
            it[supportedVersions] = stub.supportedVersions
            it[externalIp] = stub.externalIp
            it[internalIp] = stub.internalIp
            it[modded] = stub.modded
            it[platform] = stub.platform
            it[modpackUrl] = stub.modpackUrl
            it[modloader] = stub.modloader
        } > 0
    }

    override suspend fun addNewServer(
        name: String,
        supportedVersions: String,
        externalIp: String,
        internalIp: String?,
        modded: Boolean,
        platform: Platform,
        modpackUrl: String?,
        modloader: String?
    ): Server? = dbQuery {
        logger.trace { "addNewServer {$name:" }
        logger.trace { "$supportedVersions, $externalIp, $internalIp" }
        logger.trace { "$modded, $platform, $modpackUrl, $modloader}" }
        val insertStatement = Servers.insert {
            it[Servers.name] = name
            it[Servers.supportedVersions] = supportedVersions
            it[Servers.externalIp] = externalIp
            it[Servers.internalIp] = internalIp
            it[Servers.modded] = modded
            it[Servers.platform] = platform
            it[Servers.modpackUrl] = modpackUrl
            it[Servers.modloader] = modloader
        }

        insertStatement.resultedValues?.singleOrNull()?.intoServer()
    }

    override suspend fun deleteServer(id: UUID): Boolean = dbQuery {
        logger.trace { "deleteServer[$id]" }
        Connections.deleteWhere { server eq id }
        Servers.deleteWhere { Servers.id eq id } > 0
    }

    override suspend fun allConnections(): List<Connection> = dbQuery {
        logger.trace { "allConnections" }
        Connections.selectAll().map { it.intoConnection() }
    }

    override suspend fun connectionsFromServer(serverId: UUID): List<Connection> = dbQuery {
        logger.trace { "connectionsFromServer[$serverId]" }
        Connections.select { Connections.server eq serverId }.map { it.intoConnection() }
    }

    override suspend fun connection(playerUUID: UUID): Connection? = dbQuery {
        logger.trace { "connection[$playerUUID]" }
        Connections.select { Connections.player eq playerUUID }.map { it.intoConnection() }.singleOrNull()
    }

    override suspend fun addNewConnection(playerId: UUID, version: String, serverId: UUID): Connection? = dbQuery {
        logger.trace { "addNewConnection[$playerId, $version, $serverId]" }
        Connections.insert {
            it[player] = playerId
            it[Connections.version] = version
            it[server] = serverId
        }.resultedValues?.singleOrNull()?.intoConnection()
    }

    override suspend fun deleteConnection(playerId: UUID): Boolean = dbQuery {
        logger.trace { "deleteConnection[$playerId]" }
        Connections.deleteWhere { player eq playerId } > 0
    }

    override suspend fun getNonceOwner(nonce: String): UUID? = dbQuery {
        logger.trace { "getNonceOwner" }
        Nonces.select { Nonces.nonce eq nonce }.firstOrNull()?.intoNonce()?.playerId
    }

    override suspend fun getNonceOf(uuid: UUID): String? = dbQuery {
        logger.trace { "getNonceOf" }
        Nonces.select { Nonces.uuid eq uuid }.firstOrNull()?.intoNonce()?.nonce
    }

    override suspend fun clearExistingNonceOf(uuid: UUID): Boolean = dbQuery {
        logger.trace { "clearExistingNonceOf[$uuid]" }
        Nonces.deleteWhere { Nonces.uuid eq uuid } > 1
    }

    override suspend fun addNewNonce(nonce: String, uuid: UUID): Nonce? = dbQuery {
        logger.trace { "addNewNonce" }
        Nonces.insert {
            it[Nonces.nonce] = nonce
            it[Nonces.uuid] = uuid
        }.resultedValues?.firstOrNull()?.intoNonce()
    }

    override suspend fun getUser(id: ULong): DiscordUser? = dbQuery {
        logger.trace { "getUser[$id]" }
        Discord.select { Discord.id eq id }.firstOrNull()?.intoDiscordUser()
    }

    override suspend fun updateUser(user: DiscordUser): Boolean = dbQuery {
        logger.trace { "updateUser[${user.id}, ...]" }
        Discord.update({ Discord.id eq user.id }) {
            it[username] = user.username
            it[discriminator] = user.discriminator
            it[nickname] = user.nickname
            it[email] = user.email
            it[joinedAt] = user.joinedAt.toJavaInstant()
            it[accessToken] = user.accessToken
            it[refreshToken] = user.refreshToken
            it[expiresAt] = user.expiresAt.toJavaInstant()
        } > 0
    }

    override suspend fun removeUser(id: ULong): Boolean = dbQuery {
        logger.trace { "removeUser[$.id]" }
        Discord.deleteWhere { Discord.id eq id } > 0
    }

    override suspend fun addNewUser(user: DiscordUser): DiscordUser? = dbQuery {
        logger.trace { "addNewUser{${user.id}, ...}" }
        Discord.insert {
            it[id] = user.id
            it[username] = user.username
            it[discriminator] = user.discriminator
            it[nickname] = user.nickname
            it[email] = user.email
            it[joinedAt] = user.joinedAt.toJavaInstant()
            it[accessToken] = user.accessToken
            it[refreshToken] = user.refreshToken
            it[expiresAt] = user.expiresAt.toJavaInstant()
        }.resultedValues?.firstOrNull()?.intoDiscordUser()
    }
}