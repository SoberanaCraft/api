package net.soberanacraft.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.*
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.exposed.sql.update
import net.soberanacraft.api.dao.DaoFacadeImpl
import net.soberanacraft.api.discord.client.DiscordAPI
import net.soberanacraft.api.discord.client.DiscordOAuth
import net.soberanacraft.api.models.*
import net.soberanacraft.api.ws.playerLinkWebSocket
import java.util.*

val dao = DaoFacadeImpl()

fun Application.configureRouting() {
    routing {
        /// Open Routes

        get("/players") {
            val players = dao.allPlayers()
            call.respond(players)
        }

        get("/player/{uuid?}") {
            val id =
                call.parameters["uuid"] ?: return@get call.respond(ErrorMessage("Missing uuid"))
            val uuid = id.safeInto() ?: return@get call.respond(InvalidUUIDMessage("uuid", id))
            val player = dao.player(uuid) ?: return@get call.respond(ErrorMessage("No player with uuid: $id"))
            call.respond(player)
        }

        get("/link") {
            val code = call.request.queryParameters["code"] ?: return@get call.respond(ErrorMessage("Missing code."))
            val state = call.request.queryParameters["state"] ?: return@get call.respond(ErrorMessage("Missing state."))
            val playerUUID = dao.getNonceOwner(state)
                ?: return@get call.respond(ErrorMessage("No player found for the given nonce."))
            val player = dao.player(playerUUID)
                ?: return@get call.respond(ErrorMessage("A nonce was matched but the player didn't exist."))

            if (player.discordId != null)
                return@get call.respond(LinkMessage(player.uuid, LinkStatus.AlreadyLinked, player.discordId, player.joinedAt))

            val tokens = DiscordOAuth.getTokens(code) ?: return@get call.flowRespond(
                LinkMessage(
                    playerUUID,
                    LinkStatus.InvalidDiscord,
                    null,
                    null
                ),
                Flows.LinkMessage
            )

            val api = DiscordAPI(tokens.accessToken)
            val user = api.user() ?: return@get call.flowRespond(
                LinkMessage(
                    playerUUID,
                    LinkStatus.InvalidDiscord,
                    null,
                    null
                ),
                Flows.LinkMessage
            )

            val member = api.member(828778305691844609UL) ?: return@get call.flowRespond(
                LinkMessage(
                    playerUUID,
                    LinkStatus.NotJoinedToGuild,
                    user.id,
                    null
                ),
                Flows.LinkMessage
            )

            val discordUser = user.into(member.joinedAt, member.nickname, tokens)
            dao.addNewUser(discordUser)
            dao.clearExistingNonceOf(playerUUID)
            dao.link(playerUUID, discordUser.id, discordUser.trust())

            return@get call.flowRespond(LinkMessage(playerUUID, LinkStatus.JoinedGuild, user.id, member.joinedAt), Flows.LinkMessage)
        }

        get("/nonce") {
            return@get call.respond(RandomStringUtils.randomAlphanumeric(32))
        }

        get("/extnonce") {
            return@get call.respond(
                "${RandomStringUtils.randomAlphanumeric(26)}.${
                    RandomStringUtils.randomAlphanumeric(
                        6
                    )
                }.${RandomStringUtils.randomAlphanumeric(38)}"
            )
        }

        get("/auth") {
            val state = call.request.queryParameters["state"] ?: return@get call.respond(ErrorMessage("Missing state."))
            return@get call.respond(DiscordOAuth.getAuthorizationUrl(state))
        }

        get("/player/isAvailable/{username?}") {
            val username = call.parameters["username"] ?: return@get call.respond(
                ErrorMessage("Missing username")
            )
            call.respond(dao.playerByNickname(username) == null)
        }

        get("/onlines") {
            call.respond(dao.allConnections())
        }

        get("/online/{server?}") {
            val server = call.parameters["server"] ?: return@get call.respond(
                ErrorMessage("Missing server")
            )
            call.respond(
                dao.connectionsFromServer(
                    serverId = server.safeInto() ?: return@get call.respond(
                        InvalidUUIDMessage("server", server)
                    )
                )
            )
        }

        get("/servers") {
            call.respond(dao.allServers())
        }

        get("/server/{id?}") {
            val id =
                call.parameters["id"] ?: return@get call.respond(ErrorMessage("Missing id"))
            val server = dao.server(id.safeInto() ?: return@get call.respond(InvalidUUIDMessage("id", id)))
                ?: return@get call.respond(
                    ErrorMessage("No server found with id: $id")
                )
            call.respond(server)
        }
        /// WebSocket
        playerLinkWebSocket()

        /// Reserved Routes
        authenticate("auth-bearer") {
            get("/@me") {
                call.respond("Hello ${call.principal<UserIdPrincipal>()?.name}!")
            }

            post("/player/create") {
                val player = call.receive<PlayerStub>()
                val newPlayer = dao.addNewPlayer(player.uuid, player.nickname, player.platform)
                    ?: call.respond(ErrorMessage("A player with uuid: ${player.uuid} already exists."))
                call.respond(newPlayer)
            }

            post("/player/refer/{uuid?}/{referee?}") {
                val playerId = call.parameters["uuid"] ?: return@post call.respond(ErrorMessage("Missing uuid."))
                val refereeId = call.parameters["referee"] ?: return@post call.respond(ErrorMessage("Missing referee."))

                val player =
                    dao.player(playerId.safeInto() ?: return@post call.respond(InvalidUUIDMessage("uuid", playerId)))
                        ?: return@post call.respond(ErrorMessage("Player with uuid: $playerId does not exist."))
                if (player.referer != null) return@post call.respond(ErrorMessage("Player ${player.nickname} (${player.uuid}) already has a referee (${player.referer})."))
                if (player.trustFactor != Trust.Unlinked) return@post call.respond(ErrorMessage("Player ${player.nickname} (${player.uuid}) already has write access."))

                val referee = dao.player(
                    refereeId.safeInto() ?: return@post call.respond(
                        InvalidUUIDMessage(
                            "referee",
                            refereeId
                        )
                    )
                )
                    ?: return@post call.respond(ErrorMessage("Player with uuid: $refereeId does not exist."))

                val update = Players.update({ Players.uuid eq player.uuid }) {
                    it[referer] = referee.uuid
                    it[trustFactor] = Trust.Reffered
                }

                return@post call.respond(update > 0)
            }


            delete("/player/revoke") {
                val code =
                    call.request.queryParameters["uuid"] ?: return@delete call.respond(ErrorMessage("Missing uuid."))
                val uuid = code.safeInto() ?: return@delete call.respond(InvalidUUIDMessage("uuid", code))

                val player = dao.player(uuid) ?: return@delete call.respond(ErrorMessage("Player with UUID $uuid could not be found."))

                if(player.discordId != null) {
                    dao.removeUser(player.discordId)
                    dao.removeAuthenticatedUser(uuid)
                }

                dao.deleteConnection(uuid)

                return@delete call.respond(dao.deletePlayer(uuid))
            }

            post("/player/auth") {
                val code =
                    call.request.queryParameters["owner"] ?: return@post call.respond(ErrorMessage("Missing owner."))

                val code2 =
                    call.request.queryParameters["password"] ?: return@post call.respond(ErrorMessage("Missing password."))

                val playerUUID = code.safeInto() ?: return@post call.respond(InvalidUUIDMessage("owner", code))

                return@post call.respond(dao.authenticate(playerUUID, code2))
            }

            post("/player/register") {
                val code =
                    call.request.queryParameters["owner"] ?: return@post call.respond(ErrorMessage("Missing owner."))

                val code2 =
                    call.request.queryParameters["discordId"] ?: return@post call.respond(ErrorMessage("Missing discordId."))

                val code3 =
                    call.request.queryParameters["password"] ?: return@post call.respond(ErrorMessage("Missing password."))

                val playerUUID = code.safeInto() ?: return@post call.respond(InvalidUUIDMessage("owner", code))
                dao.getUser(code2.toULong()) ?: return@post call.respond(ErrorMessage("Discord user with ID $code2 not found."))

                val user = dao.createNewAuthenticatedUser(playerUUID, code2.toULong(), code3) ?: return@post call.respond(ErrorMessage("Player with UUID: $playerUUID could not be found."))

                return@post call.respond(user)
            }

            post("/player/auth/update") {
                val code =
                    call.request.queryParameters["owner"] ?: return@post call.respond(ErrorMessage("Missing owner."))

                val code2 =
                    call.request.queryParameters["old"] ?: return@post call.respond(ErrorMessage("Missing old."))

                val code3 =
                    call.request.queryParameters["new"] ?: return@post call.respond(ErrorMessage("Missing new."))

                val playerUUID = code.safeInto() ?: return@post call.respond(InvalidUUIDMessage("owner", code))

                return@post call.respond(dao.updatePassword(playerUUID, code2, code3))
            }

            delete("/player/unregister") {
                val code =
                    call.request.queryParameters["owner"] ?: return@delete call.respond(ErrorMessage("Missing owner."))

                val playerUUID = code.safeInto() ?: return@delete call.respond(InvalidUUIDMessage("owner", code))

                return@delete call.respond(dao.removeAuthenticatedUser(playerUUID))
            }

            post("/nonce/create") {
                val uuid =
                    call.request.queryParameters["uuid"] ?: return@post call.respond(ErrorMessage("Missing uuid."))
                val nonce =
                    call.request.queryParameters["nonce"] ?: return@post call.respond(ErrorMessage("Missing nonce."))
                dao.clearExistingNonceOf(uuid.safeInto() ?: return@post call.respond(InvalidUUIDMessage("uuid", uuid)))
                //NOTE: Usar `into` diretamente aqui é safe porque o uuid é checkado no `clearExistingNonceOf`.
                val newNonce = dao.addNewNonce(nonce, uuid.into())
                    ?: return@post call.respond(ErrorMessage("An error occurred while processing your nonce."))
                call.respond(newNonce)
            }

            delete("/nonce/revoke") {
                val uuid =
                    call.request.queryParameters["uuid"] ?: return@delete call.respond(ErrorMessage("Missing uuid."))
                dao.clearExistingNonceOf(
                    uuid.safeInto() ?: return@delete call.respond(
                        InvalidUUIDMessage(
                            "uuid",
                            uuid
                        )
                    )
                )
                call.respond(true)
            }

            post("/server/join") {
                val connection = call.receive<Connection>()
                val player = dao.player(connection.playerUUID) ?: return@post call.respond(
                    ErrorMessage("No player found with UUID: ${connection.playerUUID}"),
                )
                val server = dao.server(connection.serverId) ?: return@post call.respond(
                    ErrorMessage("No server found with id: ${connection.serverId}"),
                )

                val existingConnection = dao.connection(player.uuid)

                if (existingConnection != null) {
                    if (existingConnection.serverId == server.id) return@post call.respond(
                        ErrorMessage("The player is already connected to this server."),
                    )
                    else {
                        val newCount = dao.server(existingConnection.serverId)?.playerCount?.minus(1) ?: 0
                        dao.updatePlayerCount(existingConnection.serverId, newCount)
                        dao.deleteConnection(playerId = existingConnection.playerUUID)
                    }
                }

                dao.addNewConnection(connection.playerUUID, connection.version, connection.serverId)
                dao.updatePlayerCount(connection.playerUUID, server.playerCount + 1)
                call.respond(SucessMessage("Player: ${player.nickname} (${player.uuid}) connected to Server: ${server.name} added/updated."))
            }

            post("/server/disconnect") {
                val connection = call.receive<Connection>()

                val player = dao.player(connection.playerUUID) ?: return@post call.respond(
                    ErrorMessage("No player found with UUID: ${connection.playerUUID}")
                )
                val server = dao.server(connection.serverId) ?: return@post call.respond(
                    ErrorMessage("No server found with id: ${connection.serverId}")
                )

                val existingConnection = dao.connection(player.uuid) ?: return@post call.respond(
                    ErrorMessage("The player: ${player.nickname} (${player.uuid}) is not connected to any servers.")
                )

                if (existingConnection.serverId != server.id) return@post call.respond(
                    ErrorMessage("The player: ${player.nickname} (${player.uuid}) is not connected to the provided server: ${server.name} (${server.id})."),
                )
                dao.deleteConnection(player.uuid)
                dao.updatePlayerCount(server.id, server.playerCount - 1)
                call.respond(SucessMessage("Player: ${player.nickname} (${player.uuid}) disconnected from Server: ${server.name}."))
            }

            post("/server/update") {
                val server = call.receive<ServerStub>()
                val existingServer = dao.serverByName(server.name)
                    ?: return@post call.respond(
                        ErrorMessage("A server with name: ${server.name} doesn't exist.")
                    )
                return@post call.respond(dao.updateServer(existingServer.id, server))
            }

            post("/server/create") {
                val server = call.receive<ServerStub>()
                val existingServer = dao.serverByName(server.name)
                if (existingServer != null) return@post call.respond(
                    ErrorMessage("A server with name: ${server.name} (${existingServer.id}) is already registered.")
                )
                val newServer = dao.addNewServer(
                    server.name,
                    server.supportedVersions,
                    server.externalIp,
                    server.internalIp,
                    server.modded,
                    server.platform,
                    server.modpackUrl,
                    server.modloader
                ) ?: call.respond(
                    ErrorMessage("An internal error occoured while adding the server.")
                )
                call.respond(newServer)
            }

            delete("/server/revoke") {
                val server = call.parameters["id"] ?: return@delete call.respond(
                    ErrorMessage("Missing id")
                )
                val status =
                    dao.deleteServer(server.safeInto() ?: return@delete call.respond(InvalidUUIDMessage("id", server)))
                call.respond(status)
            }
        }
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.flowRespond(value: T, flow: MutableSharedFlow<T>) {
    respond(value)
    flow.emit(value)
}

private fun String.into(): UUID = UUID.fromString(this)

fun String.safeInto(): UUID? {
    return try {
        this.into()
    } catch (_: IllegalArgumentException) {
        null
    }
}
