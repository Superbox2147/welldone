package org.whatever

import com.sksamuel.scrimage.color.RGBColor
import dev.kord.common.Color
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.embed
import org.whatever.discordutils.RespondUtilities
import org.whatever.discordutils.SharedCommandParts
import org.whatever.exceptions.ImageTooBigException
import org.whatever.images.Difference
import org.whatever.images.PaletteChecker
import org.whatever.images.TemplateMask
import org.whatever.images.TemplateStyle
import org.whatever.io.loadAny
import org.whatever.io.readImageFile
import org.whatever.io.readImageUrl
import org.whatever.io.uploadGif
import org.whatever.io.uploadImage
import org.whatever.types.Palette
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI

object ApplicationCommands {
    val palette = Palette.loadFrom(File("assets/palette_c82.json"))

    private val differenceCounter = Difference()
    private val botManagers = loadBotManagers()
    private val paletteChecker = PaletteChecker(palette)

    suspend fun mask(interaction: ChatInputCommandInteraction, command: InteractionCommand) {
        val response = interaction.deferPublicResponse()
        val mainAttachment = command.attachments["image"]!!
        if (!mainAttachment.isImage) {
            RespondUtilities.respondError(
                logger,
                response,
                "Image provided was not an image",
                "Attachment included is not an image"
            )
            return
        }
        val padding = command.integers["padding"] ?: 0
        val imageUrl = URI(mainAttachment.url).toURL()
        val maskFile = command.strings["mask"]
        val maskUrl =
            command.attachments["custom-mask"]
                ?.url
                ?.let { URI(it) }
                ?.toURL()
        val mask = if (maskFile != null) {
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
                RespondUtilities.respondError(
                    logger,
                    response,
                    "Attempted to process null image",
                    "Could not load mask image"
                )
                return
            }
        }

        val image =
            try {
                readImageUrl(imageUrl)
            } catch (_: NullPointerException) {
                RespondUtilities.respondError(
                    logger,
                    response,
                    "Failed to load image to mask",
                    "Failed to load template image"
                )
                return
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

    suspend fun comparePalette(interaction: ChatInputCommandInteraction, command: InteractionCommand) {
        val response = interaction.deferPublicResponse()
        val attachment = command.attachments["image"]!!
        if (!attachment.isImage) {
            RespondUtilities.respondError(
                logger,
                response,
                "Image provided was not an image",
                "Attachment included is not an image"
            )
            return
        }

        val imageUrl = URI(attachment.url).toURL()

        val images = try {
            loadAny(imageUrl, attachment.contentType!!)
        } catch (_: NullPointerException) {
            RespondUtilities.respondError(
                logger,
                response,
                "Failed to load image",
                "Failed to load image"
            )
            return
        }

        val transformedImages = mutableMapOf<BufferedImage, List<Pair<Int, Int>>>()

        val localPaletteComparer = if (command.strings["palette"] == null) {
            paletteChecker
        } else {
            try {
                val palette = Palette(
                    buildList {
                        for (colorString in command.strings["palette"]!!.split(",")
                            .map { it.trim().removePrefix("#") }) {
                            if (colorString.length == 6) {
                                add(
                                    RGBColor(
                                        colorString.take(2).toInt(radix = 16),
                                        colorString.drop(2).take(2).toInt(radix = 16),
                                        colorString.drop(4).take(2).toInt(radix = 16)
                                    )
                                )
                            } else if (colorString.length == 8) {
                                // We will assume transparency is the *last* value
                                add(
                                    RGBColor(
                                        colorString.take(2).toInt(radix = 16),
                                        colorString.drop(2).take(2).toInt(radix = 16),
                                        colorString.drop(4).take(2).toInt(radix = 16),
                                        colorString.drop(6).take(2).toInt(radix = 16)
                                    )
                                )
                            }
                            add(RGBColor(0, 0, 0, 0))
                        }
                    }
                )
                PaletteChecker(palette)
            } catch (_: Exception) {
                RespondUtilities.respondError(
                    logger,
                    response,
                    "Failed to parse palette",
                    "Failed to parse palette"
                )
                return
            }
        }

        images.forEach { image ->
            val checkResult = localPaletteComparer.check(image)
            transformedImages[checkResult.second] = checkResult.first
        }

        val embeddableImage = if (transformedImages.size == 1) {
            val entry = transformedImages.entries.first()
            uploadImage(entry.key, "check-result")
        } else {
            uploadGif(transformedImages.keys.toList(), "check-result")
        }

        if (transformedImages.size > 1) {
            // We're working with an animation, generate animation embed
            val gifDescriptionBuilder = StringBuilder()

            val totalIncorrectPixels = transformedImages.values.sumOf { it.size }

            if (totalIncorrectPixels == 0) {
                gifDescriptionBuilder.append("All pixels in all frames match the palette")
            } else {
                if (transformedImages.size <= 10) {
                    transformedImages.values.forEachIndexed { index, pixels ->
                        gifDescriptionBuilder.append(
                            "\nFrame ${index + 1}: ${
                                if (pixels.isEmpty()) {
                                    "All pixels match"
                                } else {
                                    "${pixels.size} incorrect pixels"
                                }
                            }"
                        )
                    }
                } else {
                    gifDescriptionBuilder.append("$totalIncorrectPixels incorrect pixels found across all frames")
                }
            }

            response.respond {
                embed {
                    title = "Palette check"
                    description = gifDescriptionBuilder.toString()
                    this.image = embeddableImage
                    color = Color(0, 127, 255)
                }
            }
        } else {
            // We're working with a regular image, generate a regular image embed
            val checkResult = transformedImages.entries.first().value
            if (checkResult.isEmpty()) {
                response.respond {
                    embed {
                        title = "Palette check"
                        description = "Image matches palette"
                        this.image = embeddableImage
                        color = Color(0, 127, 255)
                    }
                }
                return
            }

            response.respond {
                embed {
                    title = "Palette check"
                    description =
                        if (checkResult.size <= 10) {
                            paletteChecker.formatIncorrectPixels(checkResult)
                        } else {
                            "${checkResult.size} pixels do not match the palette"
                        }
                    this.image = embeddableImage
                    color = Color(0, 127, 255)
                }
            }
        }
    }

    suspend fun style(interaction: ChatInputCommandInteraction, command: InteractionCommand) {
        val response = interaction.deferPublicResponse()
        val attachment = command.attachments["image"]
        val link = command.strings["link"]

        val image = if (attachment != null) {
            if (!attachment.isImage) {
                RespondUtilities.respondError(
                    logger,
                    response,
                    "Image provided was not an image",
                    "Attachment included is not an image"
                )
                return
            }

            val imageUrl = URI(attachment.url).toURL()
            try {
                readImageUrl(imageUrl)
            } catch (_: NullPointerException) {
                RespondUtilities.respondError(
                    logger,
                    response,
                    "Failed to load image",
                    "Failed to load image"
                )
                return
            }
        } else {
            if (link == null) {
                RespondUtilities.respondError(
                    logger,
                    response,
                    "No link or image was provided",
                    "You must specify an image or link"
                )
                return
            }

            SharedCommandParts.sharedDetemp(
                response,
                link
            ) ?: return
        }

        val templateStyle = TemplateStyle(File(command.strings["style"]!!))
        val resultImage =
            try {
                logger.info("Processing image")
                templateStyle.stylize(image)
            } catch (_: ImageTooBigException) {
                RespondUtilities.respondError(
                    logger,
                    response,
                    "Attempted to process massive image",
                    "The specified image is too large for the style, the stylized image can be at most **2147483647 pixels**"
                )
                return
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

    suspend fun detemp(interaction: ChatInputCommandInteraction, command: InteractionCommand) {
        val response = interaction.deferPublicResponse()
        val template = command.strings["template"]!!

        val resultImage = SharedCommandParts.sharedDetemp(
            response,
            template
        ) ?: return

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

    suspend fun difference(interaction: ChatInputCommandInteraction, command: InteractionCommand) {
        val response = interaction.deferPublicResponse()

        val imageAttachments = listOf(
            command.attachments["image1"]!!,
            command.attachments["image2"]
        )

        val images = try {
            if (imageAttachments[1] == null) {
                loadAny(URI(imageAttachments[0]!!.url).toURL(), imageAttachments[0]!!.contentType!!)
            } else {
                loadAny(URI(imageAttachments[0]!!.url).toURL(), imageAttachments[0]!!.contentType!!) +
                loadAny(URI(imageAttachments[1]!!.url).toURL(), imageAttachments[1]!!.contentType!!)
            }
        } catch (_: NullPointerException) {
            RespondUtilities.respondError(
                logger,
                response,
                "Failed to load image",
                "Failed to load image"
            )
            return
        }

        if (images.size < 2) {
            RespondUtilities.respondError(
                logger,
                response,
                "Not enough frames provided",
                "You must provide 2 images or an animated GIF or WebP"
            )
        }

        val baseImages = images.dropLast(1)
        val changedImages = images.drop(1)

        val differences = baseImages.mapIndexed { index, image ->
            differenceCounter.checkDifference(image, changedImages[index])
        }

        val descriptionBuilder = StringBuilder()

        if (differences.size == 1) {
            descriptionBuilder.append("The difference between the images is ${differences.first()} pixels")
        } else {
            descriptionBuilder.append("The differences for each frame are:")
            differences.forEachIndexed { index, difference ->
                descriptionBuilder.append("\n${index + 1} -> ${index + 2}: $difference")
            }

            descriptionBuilder.append("\n${differences.size + 1} -> 1: ${differenceCounter.checkDifference(changedImages.last(), baseImages.first())}")
        }

        response.respond {
            embed {
                title = "Frame difference"
                color = Color(0, 127, 255)
                description = descriptionBuilder.toString()
            }
        }
    }

    suspend fun reloadCommands(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferEphemeralResponse()
        if (botManagers.contains(interaction.user.id)) {
            response.respond {
                embed {
                    description = "Reloading commands..."
                    color = Color(0, 127, 255)
                }
            }
            clearCommands()
            declareCommands(kord)
            return
        }
    }
}