package org.whatever.images

import com.sksamuel.scrimage.color.RGBColor
import org.whatever.logger
import java.awt.image.BufferedImage

class TemplateMask(
    private val mask: BufferedImage,
) {
    private val transparent = RGBColor(0, 0, 0, 0).toARGBInt()

    operator fun get(
        x: Int,
        y: Int,
    ): Boolean = RGBColor.fromARGBInt(mask.getRGB(x % mask.width, y % mask.height)).alpha != 0

    fun mask(
        image: BufferedImage,
        xOffset: Int,
        yOffset: Int,
    ): BufferedImage {
        // logger.info("Template mask started")
        try {
            for (x in 0..<image.width) {
                for (y in 0..<image.height) {
                    if (this[x + xOffset, y + yOffset]) {
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
}
