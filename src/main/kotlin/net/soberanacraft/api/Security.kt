package net.soberanacraft.api

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureSecurity() {
    install(Authentication) {
        bearer ("auth-bearer") {
            realm = "Access to restricted POST/GET endpoints"
            authenticate {cred ->
                if (cred.token == ConfigFactory.config.bearer) {
                    UserIdPrincipal("SoberanaCraftApi-Trusted")
                } else
                {
                    null
                }
            }
        }
    }
}