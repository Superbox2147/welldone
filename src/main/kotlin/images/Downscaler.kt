package org.whatever.images

import java.awt.image.BufferedImage

object Downscaler {
    private fun downscaleWithScale(images: List<BufferedImage>, scales: List<Int>): List<BufferedImage> = buildList {
        for ((index, image) in images.withIndex()) {
            val scale = scales[index]
            val downscaledImage = BufferedImage(
                image.width / scale,
                image.height / scale,
                BufferedImage.TYPE_INT_ARGB
            )

            for (x in 0..<image.width) {
                for (y in 0..<image.height) {
                    if (x % scale == 0 && y % scale == 0) {
                        downscaledImage.setRGB(x / scale, y / scale, image.getRGB(x, y))
                    }
                }
            }

            add(downscaledImage)
        }
    }

    fun downscale(images: List<BufferedImage>, scales: List<Int?>): List<BufferedImage> {
        return downscaleWithScale(images, scales.mapIndexed {
            index, i -> i
            ?: getImageScale(images[index])
            ?: throw IllegalStateException("Failure to determine image scale for frame $index") })
    }

    /*
    This function is from Clueless
    https://github.com/pxlsspace/Clueless/blob/main/src/utils/image/image_utils.py
     */
    private fun getImageScale(image: BufferedImage): Int? {
        val width = image.width
        val height = image.height

        var minPixelWidth = Int.MAX_VALUE
        var minPixelHeight = Int.MAX_VALUE

        var prevX = 0
        var prevY = 0

        var previousColor: Int? = null

        // Scan horizontally
        for (y in 0..<height) {
            for (x in 0..<width) {
                val color = image.getRGB(x, y)

                if (x == 0) {
                    previousColor = color
                } else if (color != previousColor &&
                    (color ushr 24 and 0xFF) != 0
                ) {
                    // Ignore transitions where both pixels are transparent
                    val currentAlpha = color ushr 24 and 0xFF
                    val previousAlpha = previousColor?.ushr(24)?.and(0xFF)

                    if (currentAlpha != 0 || previousAlpha != 0) {
                        minPixelWidth = minOf(minPixelWidth, x - prevX)
                        previousColor = color
                        prevX = x
                    }
                }
            }
            prevX = 0
        }

        // Scan vertically
        for (x in 0..<width) {
            for (y in 0..<height) {
                val color = image.getRGB(x, y)

                if (y == 0) {
                    previousColor = color
                } else {
                    val currentAlpha = color ushr 24 and 0xFF
                    val previousAlpha = previousColor?.ushr(24)?.and(0xFF)

                    if (color != previousColor &&
                        (currentAlpha != 0 || previousAlpha != 0)
                    ) {
                        minPixelHeight = minOf(minPixelHeight, y - prevY)
                        previousColor = color
                        prevY = y
                    }
                }
            }
            prevY = 0
        }

        // Use the largest value
        val pixelSize = maxOf(minPixelWidth, minPixelHeight)

        return if (pixelSize != Int.MAX_VALUE) pixelSize else null
    }
}