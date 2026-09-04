package org.whatever.io

import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel
import org.whatever.activityConfiguration
import org.whatever.kord
import org.whatever.logger

suspend fun uploadText(
    text: String,
    name: String,
): String {
    logger.info("Upload text file")
    val textBytes = text.toByteArray()
    val channel =
        MessageChannelBehavior(
            activityConfiguration["image-spew"]!!,
            kord,
        )

    return channel
        .createMessage {
            addFile(
                "$name.txt",
                ChannelProvider(textBytes.size.toLong()) {
                    ByteReadChannel(textBytes)
                },
            )
        }.attachments
        .first()
        .url
}
