package org.whatever

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.builder.interaction.attachment
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.whatever.exceptions.TokenNotSetException
import org.whatever.io.readMasks
import org.whatever.io.readStyles
import java.io.File

object Startup {
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

    fun loadBotManagers(): List<Snowflake> {
        val managerData = Json.decodeFromString<JsonArray>(dotenv["BOT_MANAGERS"] ?: "[]")
        return buildList {
            for (i in managerData) {
                add(Snowflake(i.jsonPrimitive.content.toULong()))
            }
        }
    }

    suspend fun clearCommands(kord: Kord) {
        kord.rest.interaction.getGlobalApplicationCommands(kord.selfId).forEach {
            kord.rest.interaction.deleteGlobalApplicationCommand(kord.selfId, it.id)
        }
    }

    suspend fun declareCommands(kord: Kord) {
        val templateStyles = readStyles()
        val defaultMasks = readMasks()

        kord.createGlobalChatInputCommand(
            "mask",
            "Use an image as a mask to shape another",
        ) {
            attachment(
                "image",
                "Image to be masked",
            ) {
                required = true
            }

            string(
                "mask",
                "Use a built-in mask, overrides custom mask parameter if set",
            ) {
                required = false
                for (mask in defaultMasks) {
                    choice(
                        mask.name,
                        mask.file,
                    )
                }
            }

            attachment(
                "custom-mask",
                "Use a custom mask, built-in mask overrides this",
            ) {
                required = false
            }

            integer(
                "offset-y",
                "Offset the template mask vertically (default: 0)",
            ) {
                required = false
            }

            integer(
                "offset-x",
                "Offset the template mask horizontally (default: 0)",
            ) {
                required = false
            }

            integer(
                "padding",
                "Add transparent padding around the template mask (default: 0)",
            ) {
                required = false
            }
        }

        kord.createGlobalChatInputCommand(
            "compare-palette",
            "Compare the colors of an image to the palette",
        ) {
            attachment(
                "image",
                "Image or animation to compare to the palette",
            ) {
                required = true
            }
            string(
                "palette",
                "Custom palette (default: pxls), comma separated list of hex strings #xxxxxx(xx)"
            ) {
                required = false
            }
        }

        kord.createGlobalChatInputCommand(
            "style",
            "Convert a template to a specified source style",
        ) {
            string(
                "style",
                "Pxls template style to use for the template",
            ) {
                required = true
                for (style in templateStyles) {
                    choice(
                        style.key,
                        style.value,
                    )
                }
            }

            attachment(
                "image",
                "Image to stylize",
            ) {
                required = false
            }

            string(
                "link",
                "Stylize a template, image attachment overrides",
            ) {
                required = false
            }
        }

        kord.createGlobalChatInputCommand(
            "detemp",
            "Detemplatize a template from a link",
        ) {
            string(
                "template",
                "Template link to detemplatize",
            ) {
                required = true
            }
        }

        kord.createGlobalChatInputCommand(
            "difference",
            "Calculate the number of different pixels between two images",
        ) {
            attachment(
                "image1",
                "The first image or an animated GIF or WebP",
            ) {
                required = true
            }

            attachment(
                "image2",
                "The second image (required if image1 is not animated)",
            ) {
                required = false
            }
        }

        kord.createGlobalChatInputCommand(
            "reload-commands",
            "Reload all the commands, only available for bot managers",
        )
    }

    fun loadBwaas(): List<String> {
        val bwaaData = Json.decodeFromString<JsonArray>(File("./config/bwaa.json").readText())
        return buildList {
            for (i in bwaaData) {
                add(i.jsonPrimitive.content)
            }
        }
    }
}