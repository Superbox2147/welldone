package org.whatever.io

import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import org.whatever.activityConfiguration
import org.whatever.classExtensions.toPng
import org.whatever.kord
import org.whatever.logger
import java.awt.image.BufferedImage

suspend fun uploadImage(image: BufferedImage): String {
    logger.info("Upload image file")
    val imageByteArray = image.toPng()
    val channel =
        MessageChannelBehavior(
            activityConfiguration["image-spew"]!!,
            kord,
        )

    return channel
        .createMessage {
            addFile(
                "masked.png",
                ChannelProvider(imageByteArray.size.toLong()) {
                    ByteReadChannel(imageByteArray)
                },
            )
        }.attachments
        .first()
        .url
}
