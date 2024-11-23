package org.whatever.images

import java.awt.image.BufferedImage

class Difference {
    fun checkDifference(
        image1: BufferedImage,
        image2: BufferedImage,
    ): Long {
        val padded = addPadding(image1, image2)
        val paddedImage1 = padded.first
        val paddedImage2 = padded.second

        var totalDifference = 0L
        for (y in 0..<paddedImage1.height) {
            for (x in 0..<paddedImage1.width) {
                if (paddedImage1.getRGB(x, y) != paddedImage2.getRGB(x, y)) totalDifference++
            }
        }
        return totalDifference
    }

    private fun addPadding(
        image1: BufferedImage,
        image2: BufferedImage,
    ): Pair<BufferedImage, BufferedImage> {
        val xPadded = addPaddingX(image1, image2)
        return addPaddingY(xPadded.first, xPadded.second)
    }

    private fun addPaddingX(
        image1: BufferedImage,
        image2: BufferedImage,
    ): Pair<BufferedImage, BufferedImage> {
        if (image1.width > image2.width) {
            val newImage2 = BufferedImage(image1.width, image2.height, BufferedImage.TYPE_INT_ARGB)
            overlayBufferedImage(newImage2, image2, 0, 0)
            return Pair(image1, newImage2)
        } else {
            val newImage1 = BufferedImage(image2.width, image1.height, BufferedImage.TYPE_INT_ARGB)
            overlayBufferedImage(newImage1, image1, 0, 0)
            return Pair(newImage1, image2)
        }
    }

    private fun addPaddingY(
        image1: BufferedImage,
        image2: BufferedImage,
    ): Pair<BufferedImage, BufferedImage> {
        if (image1.height > image2.height) {
            val newImage2 = BufferedImage(image2.width, image1.height, BufferedImage.TYPE_INT_ARGB)
            overlayBufferedImage(newImage2, image2, 0, 0)
            return Pair(image1, newImage2)
        } else {
            val newImage1 = BufferedImage(image1.width, image2.height, BufferedImage.TYPE_INT_ARGB)
            overlayBufferedImage(newImage1, image1, 0, 0)
            return Pair(newImage1, image2)
        }
    }

    private fun overlayBufferedImage(
        baseImage: BufferedImage,
        overlayImage: BufferedImage,
        x: Int,
        y: Int,
    ) {
        val graphics = baseImage.createGraphics()
        graphics.drawImage(overlayImage, x, y, null)
        graphics.dispose()
    }
}
