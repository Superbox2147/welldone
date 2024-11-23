@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.whatever

import dev.kord.common.Color
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.cache.data.EmojiData
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.attachment
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import io.github.cdimascio.dotenv.dotenv
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import org.whatever.exceptions.ImageTooBigException
import org.whatever.exceptions.LinkParameterMissingException
import org.whatever.images.*
import org.whatever.io.*
import org.whatever.logger.Logger
import org.whatever.types.Palette
import java.io.File
import java.net.URI
import javax.naming.MalformedLinkException
import kotlin.system.exitProcess

val logger =
    Logger(
        File("./internal logs"),
        "${Clock.System.now().epochSeconds}",
    )
val defaultMasks = readMasks()
val dotenv = dotenv()
val kord = generateKord()
val activityConfiguration = readActivityConfig()
val emotes = readEmotes()
val welldones =
    emotes["welldone"]?.jsonArray ?: {
        logger.error("Welldone emote configs not defined")
        exitProcess(1)
    } as JsonArray
val listenEmotes =
    emotes["listen_emotes"]?.jsonArray ?: {
        logger.error("Listen emote configs not defined")
        exitProcess(1)
    } as JsonArray
val palette = Palette(File("assets/palette_c82.json"))
val paletteChecker = PaletteChecker(palette)
val templateStyles = readStyles()
val differenceCounter = Difference()

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    logger.info("start main")

    clearCommands()

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
            "Image to compare to the palette",
        ) {
            required = true
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
            "The first image",
        ) {
            required = true
        }

        attachment(
            "image2",
            "The second image",
        ) {
            required = true
        }
    }

    kord.on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command
        val commandParts = getCommandFullName(command).split(" ")
        logger.info(
            "Invoke command \"${
                commandParts.joinToString(" ")
            }\"",
        )
        when (commandParts[0]) {
            "mask" -> {
                val response = interaction.deferPublicResponse()
                val mainAttachment = command.attachments["image"]!!
                if (!mainAttachment.isImage) {
                    logger.info("Image provided was not an image")
                    response.respond {
                        embed {
                            description = "Attachment included is not an image"
                            color = Color(255, 0, 0)
                        }
                    }
                    return@on
                }
                val padding = command.integers["padding"] ?: 0
                val imageUrl = URI(mainAttachment.url).toURL()
                val maskFile = command.strings["mask"]
                val maskUrl =
                    command.attachments["custom-mask"]
                        ?.url
                        ?.let { URI(it) }
                        ?.toURL()
                val mask =
                    try {
                        if (maskFile != null) {
                            logger.info("Using mask $maskFile")
                            TemplateMask(readImageFile(File(maskFile)), padding.toInt())
                        } else {
                            if (maskUrl != null) {
                                logger.info("Using custom mask")
                                TemplateMask(
                                    readImageUrl(maskUrl),
                                    padding.toInt(),
                                )
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
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val image =
                    try {
                        readImageUrl(imageUrl)
                    } catch (e: NullPointerException) {
                        logger.warning("Failed to load image to mask")
                        response.respond {
                            embed {
                                description = "Failed to load template image"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val offsetX = command.integers["offset-x"] ?: 0
                val offsetY = command.integers["offset-y"] ?: 0
                logger.info("Invoke template mask with y-offset = $offsetY and x-offset = $offsetX")
                val resultImage = mask.mask(image, offsetX.toInt(), offsetY.toInt())
                val embeddableImage = uploadImage(resultImage, "masked")
                response.respond {
                    embed {
                        title = "Template mask"
                        this.image = embeddableImage
                        color = Color(0, 127, 255)
                    }
                }
                logger.info("Completed template mask")
            }
            "compare-palette" -> {
                val response = interaction.deferPublicResponse()
                val attachment = command.attachments["image"]!!
                if (!attachment.isImage) {
                    logger.info("Image provided was not an image")
                    response.respond {
                        embed {
                            description = "Attachment included is not an image"
                            color = Color(255, 0, 0)
                        }
                    }
                    return@on
                }
                val imageUrl = URI(attachment.url).toURL()
                val image =
                    try {
                        readImageUrl(imageUrl)
                    } catch (e: NullPointerException) {
                        logger.warning("Failed to load image")
                        response.respond {
                            embed {
                                description = "Failed to load image"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val checkResult = paletteChecker.check(image)
                val embeddableImage = uploadImage(checkResult.second, "check-result")
                if (checkResult.first.isEmpty()) {
                    response.respond {
                        embed {
                            title = "Palette check"
                            description = "Image matches palette"
                            this.image = embeddableImage
                            color = Color(0, 127, 255)
                        }
                    }
                    return@on
                }
                val wrongPixels = checkResult.first
                try {
                    val formattedWrongPixels = paletteChecker.formatIncorrectPixels(wrongPixels)
                    response.respond {
                        embed {
                            title = "Palette check"
                            description =
                                if (formattedWrongPixels.length <=
                                    200
                                ) {
                                    formattedWrongPixels
                                } else {
                                    "${wrongPixels.size} pixels do not match the palette"
                                }
                            this.image = embeddableImage
                            color = Color(0, 127, 255)
                        }
                    }
                } catch (e: Exception) {
                    logger.exception(e)
                }
            }
            "style" -> {
                val response = interaction.deferPublicResponse()
                val attachment = command.attachments["image"]
                if (attachment != null) {
                    if (!attachment.isImage) {
                        logger.info("Image provided was not an image")
                        response.respond {
                            embed {
                                description = "Attachment included is not an image"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                    val imageUrl = URI(attachment.url).toURL()
                    val image =
                        try {
                            readImageUrl(imageUrl)
                        } catch (e: NullPointerException) {
                            logger.warning("Failed to load image")
                            response.respond {
                                embed {
                                    description = "Failed to load image"
                                    color = Color(255, 0, 0)
                                }
                            }
                            return@on
                        }
                    val templateStyle = TemplateStyle(File(command.strings["style"]!!))
                    val resultImage =
                        try {
                            logger.info("Processing image")
                            templateStyle.stylize(image)
                        } catch (e: ImageTooBigException) {
                            logger.warning("Attempted to process massive image")
                            response.respond {
                                embed {
                                    description =
                                        "The specified image is too large for the style, the stylized image can be at most **2147483647 pixels**"
                                    color = Color(255, 0, 0)
                                }
                            }
                            return@on
                        }
                    val embeddableImage = uploadImage(resultImage, "stylized")
                    response.respond {
                        embed {
                            title = "Stylized template"
                            this.image = embeddableImage
                            color = Color(0, 127, 255)
                        }
                        actionRow {
                            this.linkButton(
                                embeddableImage,
                            ) {
                                this.label = "Image link"
                            }
                        }
                    }
                    return@on
                }
                val link = command.strings["link"]
                if (link == null) {
                    logger.warning("No link or image was provided")
                    response.respond {
                        embed {
                            description = "You must specify an image or link"
                            color = Color(255, 0, 0)
                        }
                    }
                    return@on
                }
                val image =
                    try {
                        val detemplatizer = Detemplatizer.getDetemplatizer(link)
                        detemplatizer.detemplatize(link)
                    } catch (e: LinkParameterMissingException) {
                        logger.warning("Encountered malformed link (missing params branch)")
                        response.respond {
                            embed {
                                description = "Link is missing parameters"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    } catch (e: MalformedLinkException) {
                        logger.warning("Encountered malformed link (malformed branch)")
                        response.respond {
                            embed {
                                description = "Link is malformed"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val templateStyle = TemplateStyle(File(command.strings["style"]!!))
                val resultImage =
                    try {
                        logger.info("Processing image")
                        templateStyle.stylize(image)
                    } catch (e: ImageTooBigException) {
                        logger.warning("Attempted to process massive image")
                        response.respond {
                            embed {
                                description =
                                    "The specified image is too large for the style, the stylized image can be at most **2147483647 pixels**"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val embeddableImage = uploadImage(resultImage, "stylized")
                response.respond {
                    embed {
                        title = "Stylized template"
                        this.image = embeddableImage
                        color = Color(0, 127, 255)
                    }
                    actionRow {
                        this.linkButton(
                            embeddableImage,
                        ) {
                            this.label = "Image link"
                        }
                    }
                }
            }
            "detemp" -> {
                val response = interaction.deferPublicResponse()
                val template =
                    try {
                        command.strings["template"]!!
                    } catch (e: NullPointerException) {
                        logger.error("Unknown error")
                        response.respond {
                            embed {
                                description = "Unknown error"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val resultImage =
                    try {
                        val detemplatizer = Detemplatizer.getDetemplatizer(template)
                        detemplatizer.detemplatize(template)
                    } catch (e: LinkParameterMissingException) {
                        logger.warning("Encountered malformed link (missing params branch)")
                        response.respond {
                            embed {
                                description = "Link is missing parameters"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    } catch (e: MalformedLinkException) {
                        logger.warning("Encountered malformed link (malformed branch)")
                        response.respond {
                            embed {
                                description = "Link is malformed"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val embeddableImage = uploadImage(resultImage, "detemplatized")
                response.respond {
                    embed {
                        title = "Detemplatize"
                        this.image = embeddableImage
                        color = Color(0, 127, 255)
                    }
                    actionRow {
                        this.linkButton(
                            embeddableImage,
                        ) {
                            this.label = "Image link"
                        }
                    }
                }
            }
            "difference" -> {
                val response = interaction.deferPublicResponse()
                val image1Attachment = command.attachments["image1"]!!
                val image2Attachment = command.attachments["image2"]!!
                if (!image1Attachment.isImage) {
                    logger.info("Image 1 was not an image")
                    response.respond {
                        embed {
                            description = "Attachment included is not an image (image1)"
                            color = Color(255, 0, 0)
                        }
                    }
                    return@on
                }
                if (!image2Attachment.isImage) {
                    logger.info("Image 2 was not an image")
                    response.respond {
                        embed {
                            description = "Attachment included is not an image (image2)"
                            color = Color(255, 0, 0)
                        }
                    }
                    return@on
                }
                val imageUrl1 = URI(image1Attachment.url).toURL()
                val image1 =
                    try {
                        readImageUrl(imageUrl1)
                    } catch (e: NullPointerException) {
                        logger.warning("Failed to load image1")
                        response.respond {
                            embed {
                                description = "Failed to load image1"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val imageUrl2 = URI(image2Attachment.url).toURL()
                val image2 =
                    try {
                        readImageUrl(imageUrl2)
                    } catch (e: NullPointerException) {
                        logger.warning("Failed to load image2")
                        response.respond {
                            embed {
                                description = "Failed to load image2"
                                color = Color(255, 0, 0)
                            }
                        }
                        return@on
                    }
                val difference = differenceCounter.checkDifference(image1, image2)
                response.respond {
                    embed {
                        title = "Image difference"
                        color = Color(0, 127, 255)
                        description = "The difference between the images is $difference pixels"
                    }
                }
            }
        }
    }

    kord.on<MessageCreateEvent> {
        if (message.content.startsWith("<@${kord.selfId.value}>")) {
            message.reply {
                content = welldones.random().jsonPrimitive.content
            }
            logger.info("Welldone")
            return@on
        }
        for (item in listenEmotes) {
            val emote = item.jsonObject["emote"]!!
            val response = item.jsonObject["response"]!!
            if (message.content.startsWith(emote.jsonPrimitive.content)) {
                val emoteId =
                    Snowflake(
                        response.jsonObject["id"]!!
                            .jsonPrimitive.content
                            .toULong(),
                    )
                val emoteName =
                    response.jsonObject["name"]!!
                        .jsonPrimitive.content
                val guildId =
                    Snowflake(
                        response.jsonObject["guild"]!!
                            .jsonPrimitive.content
                            .toULong(),
                    )
                message.addReaction(
                    GuildEmoji(
                        EmojiData(
                            id = emoteId,
                            name = emoteName,
                            guildId = guildId,
                        ),
                        kord,
                    ),
                )
                logger.info("Welldone")
                return@on
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

fun getCommandFullName(command: InteractionCommand): String {
    if (command.data.options.value
            ?.first()
            ?.value
            ?.value != null
    ) {
        return command.rootName
    }
    if (command.data.options.value
            ?.first() == null
    ) {
        return command.rootName
    }
    return "${command.rootName} ${command.data.options.value?.first()?.name}"
}
