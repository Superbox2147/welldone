package org.whatever.classExtensions

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun BufferedImage.toPng(): ByteArray {
    val outputStream = ByteArrayOutputStream(65535)
    ImageIO.write(this, "png", outputStream)
    return outputStream.toByteArray()
}