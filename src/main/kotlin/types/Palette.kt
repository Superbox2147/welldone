package org.whatever.types

import com.sksamuel.scrimage.color.RGBColor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.whatever.logger
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

class Palette(
    private val palette: List<RGBColor>
) {
    fun contains(color: RGBColor): Boolean = palette.contains(color)

    fun indexOf(color: RGBColor?) = palette.indexOf(color)

    operator fun get(index: Int): RGBColor = palette[index]

    companion object {
        fun loadFrom(file: File): Palette {
            try {
                val paletteJson = Json.decodeFromString<JsonObject>(file.readText())
                    return Palette(buildList {
                        for ((_, value) in paletteJson) {
                            val colorString =
                                value.jsonPrimitive.content
                                    .drop(1)
                            val red = (colorString[0].toString() + colorString[1]).toInt(radix = 16)
                            val green = (colorString[2].toString() + colorString[3]).toInt(radix = 16)
                            val blue = (colorString[4].toString() + colorString[5]).toInt(radix = 16)
                            add(RGBColor(red, green, blue, 255))
                        }
                        add(RGBColor(0, 0, 0, 0))
                    })
            } catch (_: FileNotFoundException) {
                logger.error("Palette file does not exist")
                exitProcess(1)
            } catch (_: SerializationException) {
                logger.error("Palette file is malformed")
                exitProcess(1)
            }
        }
    }
}
