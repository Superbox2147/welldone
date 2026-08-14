package org.whatever.images

import java.awt.image.BufferedImage

object Difference {
    fun checkDifference(
        image1: BufferedImage,
        image2: BufferedImage,
    ): Long {
        var totalDifference = 0L
        for (y in 0..<image1.height) {
            for (x in 0..<image1.width) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) totalDifference++
            }
        }
        return totalDifference
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
