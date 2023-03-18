package net.soberanacraft.api.ws

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import net.soberanacraft.api.Flows
import net.soberanacraft.api.dao
import net.soberanacraft.api.models.ErrorMessage
import net.soberanacraft.api.models.InvalidUUIDMessage
import net.soberanacraft.api.safeInto

fun Routing.playerLinkWebSocket() {
    webSocket("/ws/link/{id?}") {
        val id = call.parameters["id"] ?: return@webSocket sendSerialized(ErrorMessage("Missing id"))
        val uuid = id.safeInto() ?: return@webSocket  sendSerialized(InvalidUUIDMessage("id", id))
        if (dao.player(uuid) == null)  return@webSocket sendSerialized(ErrorMessage("No player found for UUID: $uuid"))

        var sent = false

        while (!sent) {
            val response = Flows.LinkMessage.filter { it -> it.player == uuid }.firstOrNull()
            sent = response != null
            if (response != null) {
                sendSerialized(response)
            }
        }
    }
}