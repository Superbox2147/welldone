package org.whatever.images

import com.sksamuel.scrimage.color.RGBColor
import org.whatever.types.Palette
import java.awt.image.BufferedImage
import kotlin.math.floor

class PaletteChecker(
    private val palette: Palette,
) {
    fun check(image: BufferedImage): Pair<List<Pair<Int, Int>>, BufferedImage> {
        val colorMatches =
            buildList {
                for (x in 0..<image.width) {
                    for (y in 0..<image.height) {
                        val color = RGBColor.fromARGBInt(image.getRGB(x, y))
                        if (!palette.contains(color)) {
                            add(Pair(x, y))
                            image.setRGB(
                                x,
                                y,
                                RGBColor(255, 0, 0, 255).toARGBInt(),
                            )
                        } else {
                            image.setRGB(
                                x,
                                y,
                                RGBColor(
                                    color.red,
                                    color.green,
                                    color.blue,
                                    floor(color.alpha * 0.5).toInt(),
                                ).toARGBInt(),
                            )
                        }
                    }
                }
            }
        return Pair(colorMatches, image)
    }

    fun formatIncorrectPixels(wrongPixels: List<Pair<Int, Int>>): String {
        val stringBuilder = StringBuilder("The following pixels do not match:")
        for (pixel in wrongPixels) {
            stringBuilder.append("(${pixel.first}, ${pixel.second})\n")
        }
        return stringBuilder.toString()
    }
}
