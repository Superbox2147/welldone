package org.whatever.io

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.whatever.types.DefaultMask
import java.io.File
import org.whatever.logger
import kotlin.system.exitProcess

fun readMasks(): List<DefaultMask> = buildList {
    logger.info("Reading masks")
    val masks = Json
        .decodeFromString<JsonArray>(
            File("./config/masks.json")
            .readText())
    for (mask in masks) {
        add(Json.decodeFromJsonElement<DefaultMask>(mask))
    }
}

fun readActivityConfig(): Map<String, Snowflake> {
    try {
        val configJson = Json.decodeFromString<JsonObject>(File("./config/activity-channels.json").readText())
        return buildMap {
            put(
                "server",
                Snowflake(
                    configJson["server"]!!
                        .jsonPrimitive.content.toULong()
                )
            )
            put(
                "image-spew",
                Snowflake(
                    configJson["channels"]!!
                        .jsonObject["image-spew"]!!
                        .jsonPrimitive.content.toULong()
                )
            )
        }
    } catch (e: NullPointerException) {
        logger.error("Value not found in activity channels config")
        exitProcess(1)
    } catch (e: NumberFormatException) {
        logger.error("Malformed value in activity channel config")
        exitProcess(1)
    } catch (e: SerializationException) {
        logger.error("Invalid config file syntax for activity channel config")
        exitProcess(1)
    }
}