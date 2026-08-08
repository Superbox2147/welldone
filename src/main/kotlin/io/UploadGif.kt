package org.whatever.io

import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.matrixlab.webp4j.WebPCodec
import dev.matrixlab.webp4j.animation.FrameNormalizer
import dev.matrixlab.webp4j.gif.GifToWebPConfig
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel
import org.whatever.activityConfiguration
import org.whatever.kord
import org.whatever.logger
import java.awt.image.BufferedImage

suspend fun uploadGif(
    baseFrames: List<BufferedImage>,
    name: String,
): String {
    logger.info("Upload GIF file")
    val channel =
        MessageChannelBehavior(
            activityConfiguration["image-spew"]!!,
            kord,
        )

    val encoderConfig = GifToWebPConfig.createLosslessConfig().apply {
        isMultiThreaded = true
        loopCount = 0
    }
    val frameDelays = IntArray(baseFrames.size) { 500 }
    val frames = FrameNormalizer.normalize(baseFrames)

    val byteArray = WebPCodec.createAnimatedWebP(frames, frameDelays, encoderConfig)

    return channel
        .createMessage {
            addFile(
                "$name.webp",
                ChannelProvider(byteArray.size.toLong()) {
                    ByteReadChannel(byteArray)
                },
            )
        }.attachments
        .first()
        .url
}