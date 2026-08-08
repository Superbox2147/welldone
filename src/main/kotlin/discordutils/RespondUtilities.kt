package org.whatever.discordutils

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.message.embed
import org.whatever.logger.Logger

object RespondUtilities {
    suspend fun respondError(
        logger: Logger,
        response: DeferredMessageInteractionResponseBehavior,
        logMessage: String,
        message: String
    ) {
        logger.warning(logMessage)
        response.respond {
            embed {
                description = message
                color = Color(255, 0, 0)
            }
        }
    }
}