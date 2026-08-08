package org.whatever.io

import com.madgag.gif.fmsware.AnimatedGifEncoder
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel
import org.whatever.activityConfiguration
import org.whatever.kord
import org.whatever.logger
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

suspend fun uploadGif(
    gif: List<BufferedImage>,
    name: String,
): String {
    logger.info("Upload GIF file")
    val channel =
        MessageChannelBehavior(
            activityConfiguration["image-spew"]!!,
            kord,
        )

    val outputStream = ByteArrayOutputStream()

    val gifEncoder = AnimatedGifEncoder()
    gifEncoder.start(outputStream)
    gifEncoder.setDelay(500)
    gif.forEach { frame ->
        gifEncoder.addFrame(frame)
    }
    gifEncoder.finish()

    val byteArray = outputStream.toByteArray()
    outputStream.close()

    return channel
        .createMessage {
            addFile(
                "$name.png",
                ChannelProvider(byteArray.size.toLong()) {
                    ByteReadChannel(byteArray)
                },
            )
        }.attachments
        .first()
        .url
}