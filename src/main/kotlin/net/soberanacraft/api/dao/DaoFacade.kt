package net.soberanacraft.api.dao

import net.soberanacraft.api.models.*
import java.util.UUID

interface DaoFacade {
    // Player DAO
    suspend fun allPlayers(): List<Player>
    suspend fun player(uuid: UUID): Player?
    suspend fun playerByNickname(nickname: String): Player?
    suspend fun addNewPlayer(uuid: UUID, nickname: String, platform: Platform): Player?
    suspend fun link(uuid: UUID, discordId: ULong, trust: Trust): Boolean
    suspend fun unlink(uuid: UUID): Boolean
    suspend fun refer(uuid: UUID, referee: UUID): Boolean
    suspend fun deletePlayer(uuid: UUID): Boolean

    //Server DAO
    suspend fun allServers(): List<Server>
    suspend fun server(id: UUID): Server?
    suspend fun serverByName(name: String): Server?
    suspend fun updatePlayerCount(id: UUID, count: Int): Boolean
    suspend fun updateServer(id: UUID, stub: ServerStub): Boolean
    suspend fun addNewServer(
        name: String,
        supportedVersions: String,
        externalIp: String,
        internalIp: String?,
        modded: Boolean,
        platform: Platform,
        modpackUrl: String?,
        modloader: String?
    ): Server?

    suspend fun deleteServer(id: UUID): Boolean

    // Connection DAO
    suspend fun allConnections(): List<Connection>
    suspend fun connectionsFromServer(serverId: UUID): List<Connection>
    suspend fun connection(playerUUID: UUID): Connection?
    suspend fun addNewConnection(playerId: UUID, version: String, serverId: UUID): Connection?
    suspend fun deleteConnection(playerId: UUID): Boolean

    // Nonce DAO
    suspend fun getNonceOwner(nonce: String): UUID?
    suspend fun getNonceOf(uuid: UUID): String?
    suspend fun clearExistingNonceOf(uuid: UUID): Boolean
    suspend fun addNewNonce(nonce: String, uuid: UUID): Nonce?

    // Discord DAO
    suspend fun getUser(id: ULong): DiscordUser?
    suspend fun updateUser(user: DiscordUser): Boolean
    suspend fun removeUser(id: ULong): Boolean
    suspend fun addNewUser(user: DiscordUser): DiscordUser?

    // Auth DAO
    suspend fun createNewAuthenticatedUser(owner: UUID, discordId: ULong, password: String) : AuthenticatedUser?
    suspend fun authenticate(owner: UUID, password: String) : Boolean
    suspend fun removeAuthenticatedUser(owner: UUID) : Boolean
    suspend fun updatePassword(owner: UUID, oldPassword: String, password: String) : Boolean
    suspend fun updatePassword(owner: ULong, oldPassword: String, password: String) : Boolean

}

//var player by Connections.player
//var version by Connections.version
//var servet by Connections.server