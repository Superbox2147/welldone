package org.whatever

import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.attachment
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import org.whatever.classExtensions.toPng
import org.whatever.exceptions.TokenNotSetException
import org.whatever.images.TemplateMask
import org.whatever.io.*
import org.whatever.logger.Logger
import java.io.File
import java.net.URI
import kotlin.system.exitProcess

val logger = Logger(
    File("./internal logs"),
    "${Clock.System.now().epochSeconds}"
)
val defaultMasks = readMasks()
val dotenv = dotenv()
val kord = generateKord()
val activityConfiguration = readActivityConfig()
val emotes = readEmotes()
val welldones = emotes["welldone"]?.jsonArray ?: {
    logger.error("Welldone emote configs not defines")
    exitProcess(1)
} as JsonArray

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    logger.info("start main")
    kord.createGlobalChatInputCommand(
        "mask",
        "Use an image as a mask to shape another"
    ) {

        attachment(
            "image",
            "Image to be masked"
        ) {
            required = true
        }

        string(
            "mask",
            "Use a built-in mask, overrides custom mask parameter if set"
        ) {
            required = false
            for (mask in defaultMasks) {
                choice(
                    mask.name,
                    mask.file
                )
            }
        }

        attachment(
            "custom-mask",
            "Use a custom mask, built-in mask overrides this"
        ) {
            required = false
        }
    }

    kord.on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command
        val commandParts = getCommandFullName(command).split(" ")
        logger.info("Invoke command \"${
            commandParts.joinToString(" ")
        }}")
        when (commandParts[0]) {
            "mask" -> {
                val response = interaction.deferPublicResponse()
                val mainAttachment = command.attachments["image"]!!
                if (!mainAttachment.isImage) {
                    logger.info("Image provided was not an image")
                    response.respond {
                        embed {
                            description = "Attachment included is not an image"
                        }
                    }
                    return@on
                }
                val imageUrl = URI(mainAttachment.url).toURL()
                val maskFile = command.strings["mask"]
                val maskUrl = command.attachments["custom-mask"]?.url
                    ?.let { URI(it) }?.toURL()
                val mask = try {
                    if (maskFile != null) {
                        logger.info("Using mask $maskFile")
                        TemplateMask(readImageFile(File(maskFile)))
                    } else {
                        if (maskUrl != null) {
                            logger.info("Using custom mask")
                            TemplateMask(readImageUrl(maskUrl))
                        } else {
                            logger.warning("No valid mask specified")
                            throw NullPointerException()
                        }
                    }
                } catch (e: NullPointerException) {
                    logger.warning("Attempted to process null image")
                    response.respond {
                        embed {
                            description = "Could not load mask image"
                        }
                    }
                    return@on
                }
                val image = try {
                    readImageUrl(imageUrl)
                } catch (e: NullPointerException) {
                    logger.warning("Failed to load image to mask")
                    response.respond {
                        embed {
                            description = "Failed to load template image"
                        }
                    }
                    return@on
                }
                val resultImage = mask.mask(image)
                val imageByteArray = resultImage.toPng()
                logger.info("Upload image file")
                val channel = MessageChannelBehavior(
                    activityConfiguration["image-spew"]!!,
                    kord
                )
                val embeddableImage = channel.createMessage {
                    addFile("masked.png",
                        ChannelProvider(imageByteArray.size.toLong()) {
                            ByteReadChannel(imageByteArray)
                        })
                }.attachments.first().url
                response.respond {
                    embed {
                        title = "Template mask"
                        this.image = embeddableImage
                    }
                }
                logger.info("Completed template mask")
            }
        }
    }

    kord.on<MessageCreateEvent> {
        if (message.content.startsWith("<@${kord.selfId.value}>")) {
            message.reply {
                content = welldones.random().jsonPrimitive.content
            }
        }
    }

    logger.info("Logging in as ${kord.getSelf().username}")
    kord.login {
        intents += Intent.MessageContent
        presence {
            status = PresenceStatus.Online
        }
    }
}

fun generateKord() = runBlocking {
    logger.info("Getting Kord")
    val token = dotenv["TOKEN"] ?: ""
    if (token == "") {
        logger.error("Token is not set")
        throw TokenNotSetException()
    }
    return@runBlocking Kord(token)
}

fun getCommandFullName(command: InteractionCommand): String {
    if (command.data.options.value?.first()?.value?.value != null) {
        return command.rootName
    }
    if (command.data.options.value?.first() == null) {
        return command.rootName
    }
    return "${command.rootName} ${command.data.options.value?.first()?.name}"
}