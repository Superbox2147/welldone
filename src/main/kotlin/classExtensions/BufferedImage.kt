package org.whatever.classExtensions

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

fun BufferedImage.toPng(): ByteArray {
    val outputStream = ByteArrayOutputStream(65535)
    PngWriter.MaxCompression.write(ImmutableImage.fromAwt(this), null, outputStream)
    return outputStream.toByteArray()
}
