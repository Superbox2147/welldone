package org.whatever.images

import com.sksamuel.scrimage.color.RGBColor
import org.whatever.logger
import java.awt.image.BufferedImage

class TemplateMask(
    private val mask: BufferedImage,
    private val padding: Int,
) {
    private val transparent = RGBColor(0, 0, 0, 0).toARGBInt()

    operator fun get(
        x: Int,
        y: Int,
    ): Boolean =
        try {
            RGBColor.fromARGBInt(mask.getRGB(x % mask.width + padding, y % mask.height + padding)).alpha != 0
        } catch (e: ArrayIndexOutOfBoundsException) {
            false
        }

    fun mask(
        image: BufferedImage,
        xOffset: Int,
        yOffset: Int,
    ): BufferedImage {
        // logger.info("Template mask started")
        try {
            for (x in 0..<image.width) {
                for (y in 0..<image.height) {
                    if (this[x + xOffset + padding(x, mask.width), y + yOffset + padding(y, image.height)]) {
                        continue
                    }
                    image.setRGB(x, y, transparent)
                }
            }
        } catch (e: Exception) {
            logger.exception(e)
        }
        return image
    }

    private fun padding(
        n: Int,
        dimension: Int,
    ): Int = (n / dimension) * padding
}
