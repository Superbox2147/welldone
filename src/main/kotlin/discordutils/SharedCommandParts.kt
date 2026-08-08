package org.whatever.discordutils

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.rest.builder.message.embed
import org.whatever.exceptions.LinkParameterMissingException
import org.whatever.images.Detemplatizer
import org.whatever.logger
import java.awt.image.BufferedImage
import javax.naming.MalformedLinkException

object SharedCommandParts {
    suspend fun sharedDetemp(response: DeferredMessageInteractionResponseBehavior, template: String): BufferedImage? {
        return try {
            val detemplatizer = Detemplatizer.getDetemplatizer(template)
            detemplatizer.detemplatize(template)
        } catch (_: LinkParameterMissingException) {
            RespondUtilities.respondError(
                logger,
                response,
                "Encountered malformed link (missing params branch)",
                "Link is missing parameters"
            )
            logger.warning("Encountered malformed link (missing params branch)")
            response.respond {
                embed {
                    description = "Link is missing parameters"
                    color = Color(255, 0, 0)
                }
            }
            null
        } catch (_: MalformedLinkException) {
            RespondUtilities.respondError(
                logger,
                response,
                "Encountered malformed link (malformed branch)",
                "Link is malformed"
            )
            null
        }
    }
}