package org.whatever

import dev.kord.core.behavior.channel.MessageChannelBehavior

val bwaaEmotes = loadBwaas()

suspend fun sendRandomBwaa(channel: MessageChannelBehavior) {
    logger.info("Bwaa")
    channel.createMessage(bwaaEmotes.random())
}
