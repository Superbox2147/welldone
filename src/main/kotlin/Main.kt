package org.whatever

import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.cache.data.EmojiData
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.interaction.InteractionCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.cdimascio.dotenv.dotenv
import kotlin.time.Clock
import kotlinx.serialization.json.*
import org.whatever.io.*
import org.whatever.logger.Logger
import java.io.File
import kotlin.system.exitProcess

val logger =
    Logger(
        File("./internal logs"),
        "${Clock.System.now().epochSeconds}",
    )
val dotenv = dotenv()
val kord = Startup.generateKord()
val activityConfiguration = ConfigLoaders.readActivityConfig()

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    logger.info("start main")

    val emotes = ConfigLoaders.readEmotes()
    val welldoneEmotes = ConfigLoaders.readWelldoneEmotes(emotes)

    if (welldoneEmotes == null) {
        logger.error("Welldone emote configs not defined")
        exitProcess(1)
    }

    val listenEmotes = ConfigLoaders.readOtherEmotes(emotes)

    if (listenEmotes == null) {
        logger.error("Listen emote configs not defined")
        exitProcess(1)
    }

    Startup.declareCommands(kord)

    kord.on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command
        val commandParts = getCommandFullName(command).split(" ")
        logger.info(
            "Invoke command \"${
                commandParts.joinToString(" ")
            }\"",
        )
        when (commandParts[0]) {
            "mask" -> ApplicationCommands.mask(interaction, command)
            "compare-palette" -> ApplicationCommands.comparePalette(interaction, command)
            "style" -> ApplicationCommands.style(interaction, command)
            "detemp" -> ApplicationCommands.detemp(interaction, command)
            "difference" -> ApplicationCommands.difference(interaction, command)
            "reload-commands" -> ApplicationCommands.reloadCommands(interaction)
        }
    }

    kord.on<MessageCreateEvent> {
        if (message.content.startsWith("<@${kord.selfId.value}>")) {
            message.reply {
                content = welldoneEmotes.random().jsonPrimitive.content
            }
            logger.info("Welldone")
            return@on
        }
        if (message.content.startsWith("!bwaa")) {
            Bwaa.sendRandomBwaa(message.channel)
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
