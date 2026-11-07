package org.whatever.images

import java.awt.image.BufferedImage

object Upscaler {
    fun upscale(frames: List<BufferedImage>, scale: Int): List<BufferedImage> = buildList {
        for (frame in frames) {
            val upscaledImage = BufferedImage(
                frame.width * scale,
                frame.height * scale,
                BufferedImage.TYPE_INT_ARGB
            )

            for (x in 0..<upscaledImage.width) {
                for (y in 0..<upscaledImage.height) {
                    upscaledImage.setRGB(x, y, frame.getRGB(x / scale, y / scale))
                }
            }
            add(upscaledImage)
        }
    }
}