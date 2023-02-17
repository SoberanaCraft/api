package net.soberanacraft.api.discord.client

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.soberanacraft.api.discord.model.TokensResponse
import net.soberanacraft.api.models.Config


object DiscordOAuth {
    private val logger = KotlinLogging.logger {}
    lateinit var client: HttpClient
    private lateinit var config: Config

    const val GRANT_TYPE_AUTHORIZATION = "authorization_code"
    const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"

    @OptIn(ExperimentalSerializationApi::class)
    fun init(config: Config) {
        logger.info { "Starting..." }
        this.config = config
        logger.info { "Creating new HttpClient" }
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                })
            }
        }

        logger.info { "Done" }
    }

    fun getAuthorizationUrl(state: String): String =
        URLBuilder(DiscordAPI.BASE_URI + "/oauth2/authorize").let {
            it.parameters.append("client_id", config.clientId)
            it.parameters.append("redirect_uri", config.redirectUri)
            it.parameters.append("response_type", "code")
            it.parameters.append("scope", config.scope.joinToString(" ").encodeURLParameter(false))
            if (state.isNotEmpty()) it.parameters.append("state", state)
            it
        }.buildString().replace("%2520", "%20")


    suspend fun getTokens(code: String): TokensResponse? {
        val response =
            client.submitForm(url = DiscordAPI.BASE_URI + "/oauth2/token", formParameters = Parameters.build {
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("grant_type", GRANT_TYPE_AUTHORIZATION)
                append("code", code)
                append("redirect_uri", config.redirectUri)
                append("scope", config.scope.joinToString(" "))
            })

        println("[getTokens($code)]\n" + response.bodyAsText())

        return response.nullableBody()
    }

    suspend fun refreshTokens(refreshToken: String): TokensResponse? {
        val response =
            client.submitForm(url = DiscordAPI.BASE_URI + "/oauth2/token", formParameters = Parameters.build {
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
                append("grant_type", GRANT_TYPE_REFRESH_TOKEN)
                append("refresh_token", refreshToken)
            })
        return response.nullableBody()
    }
}

suspend inline fun <reified T> HttpResponse.nullableBody(): T? {
    return try {
        this.body()
    } catch (e: Throwable) {
        println(e)
        println(e.cause)
        null
    }
}
