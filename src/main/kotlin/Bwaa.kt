package org.whatever

import dev.kord.core.behavior.channel.MessageChannelBehavior

val bwaas = loadBwaas()

suspend fun sendRandomBwaa(channel: MessageChannelBehavior) {
    logger.info("Bwaa")
    channel.createMessage(bwaas.random())
}
