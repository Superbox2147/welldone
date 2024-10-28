package org.whatever.images

import com.sksamuel.scrimage.color.RGBColor
import org.whatever.exceptions.ImageTooBigException
import org.whatever.io.readImageFile
import org.whatever.palette
import java.awt.image.BufferedImage
import java.io.File

class TemplateStyle(
    imageFile: File,
) {
    private val templateStyle: BufferedImage = readImageFile(imageFile)
    private val styleSectionsDimensions = templateStyle.width / 16

    operator fun get(index: Int): BufferedImage {
        val x = (index * styleSectionsDimensions) % templateStyle.width
        val y = ((index * styleSectionsDimensions) / templateStyle.width) * styleSectionsDimensions
        return templateStyle.getSubimage(x, y, styleSectionsDimensions, styleSectionsDimensions)
    }

    fun stylize(image: BufferedImage): BufferedImage {
        if ((image.width.toLong() * styleSectionsDimensions.toLong()) * (image.height.toLong() * styleSectionsDimensions.toLong()) >
            Int.MAX_VALUE
        ) {
            throw ImageTooBigException()
        }
        val outputImage =
            BufferedImage(image.width * styleSectionsDimensions, image.height * styleSectionsDimensions, BufferedImage.TYPE_INT_ARGB)

        for (y in 0..<image.height) {
            for (x in 0..<image.width) {
                val argb = image.getRGB(x, y)
                val pixelColor = RGBColor.fromARGBInt(argb)
                val colorIndex = palette.indexOf(pixelColor)
                if (colorIndex != -1) {
                    val overlayImage = toColor(this[colorIndex], palette[colorIndex])
                    overlayBufferedImage(outputImage, overlayImage, x * styleSectionsDimensions, y * styleSectionsDimensions)
                }
            }
        }
        return outputImage
    }

    private fun toColor(
        image: BufferedImage,
        color: RGBColor,
    ): BufferedImage {
        for (x in 0..<image.width) {
            for (y in 0..<image.height) {
                if (RGBColor.fromARGBInt(image.getRGB(x, y)).alpha == 255) {
                    image.setRGB(x, y, color.toARGBInt())
                }
            }
        }
        return image
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
