package org.whatever

import dev.kord.core.behavior.channel.MessageChannelBehavior

object Bwaa {
    private val bwaaEmotes = Startup.loadBwaas()

    suspend fun sendRandomBwaa(channel: MessageChannelBehavior) {
        logger.info("Bwaa")
        channel.createMessage(bwaaEmotes.random())
    }
}
