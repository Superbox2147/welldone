package org.whatever.images

import java.awt.image.BufferedImage

object Difference {
    fun checkDifference(
        image1: BufferedImage,
        image2: BufferedImage,
    ): Pair<Long, BufferedImage> {
        val overlayImage = BufferedImage(image1.width, image1.height, BufferedImage.TYPE_INT_ARGB)

        var totalDifference = 0L
        for (y in 0..<image1.height) {
            for (x in 0..<image1.width) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    totalDifference++
                    overlayImage.setRGB(x, y, image2.getRGB(x, y))
                }
            }
        }
        return Pair(totalDifference, overlayImage)
    }

    fun ensureCorrectFrameSizes(frames: List<BufferedImage>): Boolean {
        val baseFrames = frames.dropLast(1)
        val secondaryFrames = frames.drop(1)

        for ((index, frame) in baseFrames.withIndex()) {
            if (frame.width != secondaryFrames[index].width || frame.height != secondaryFrames[index].height) return false
        }

        return true
    }
}
