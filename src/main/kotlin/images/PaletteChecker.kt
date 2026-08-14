package org.whatever.images

import com.sksamuel.scrimage.color.HSVColor
import com.sksamuel.scrimage.color.RGBColor
import org.whatever.types.Palette
import java.awt.image.BufferedImage

class PaletteChecker(
    private val palette: Palette,
) {
    fun check(image: BufferedImage): Pair<List<Pair<Int, Int>>, BufferedImage> {
        val colorMatches =
            buildList {
                for (x in 0..<image.width) {
                    for (y in 0..<image.height) {
                        val color = RGBColor.fromARGBInt(image.getRGB(x, y)).toHSV()
                        if (!palette.contains(color.toRGB())) {
                            add(Pair(x, y))
                            image.setRGB(
                                x,
                                y,
                                RGBColor(255, 0, 0, 255).toARGBInt(),
                            )
                        } else {
                            val newColor =
                                HSVColor(
                                    color.hue,
                                    color.saturation,
                                    color.value * 0.1F,
                                    color.alpha,
                                ).toRGB()
                            image.setRGB(
                                x,
                                y,
                                newColor.toARGBInt(),
                            )
                        }
                    }
                }
            }
        return Pair(colorMatches, image)
    }

    fun formatIncorrectPixels(wrongPixels: List<Pair<Int, Int>>): String {
        val stringBuilder = StringBuilder("The following pixels do not match:")
        for ((x, y) in wrongPixels) {
            stringBuilder.append("\n($x, $y)")
        }
        return stringBuilder.toString()
    }
}
