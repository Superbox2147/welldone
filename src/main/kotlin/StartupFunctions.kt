package org.whatever

import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking
import org.whatever.exceptions.TokenNotSetException

fun generateKord() =
    runBlocking {
        logger.info("Getting Kord")
        val token = dotenv["TOKEN"] ?: ""
        if (token == "") {
            logger.error("Token is not set")
            throw TokenNotSetException()
        }
        return@runBlocking Kord(token)
    }

suspend fun clearCommands() {
    kord.rest.interaction.getGlobalApplicationCommands(kord.selfId).forEach {
        kord.rest.interaction.deleteGlobalApplicationCommand(kord.selfId, it.id)
    }
}
