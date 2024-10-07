package org.whatever.images

import com.sksamuel.scrimage.color.RGBColor
import java.awt.image.BufferedImage

class TemplateMask(private val mask: BufferedImage) {
    private val transparent = RGBColor(0, 0, 0, 0).toARGBInt()

    operator fun get(x: Int, y: Int): Boolean {
        return RGBColor.fromARGBInt(mask.getRGB(x % mask.width, y % mask.height)).alpha != 0
    }

    fun mask(image: BufferedImage): BufferedImage {
        for (x in 0..<image.width) {
            for (y in 0..<image.height) {
                if (this[x, y]) {
                    continue
                }
                image.setRGB(x, y, transparent)
            }
        }
        return image
    }
}