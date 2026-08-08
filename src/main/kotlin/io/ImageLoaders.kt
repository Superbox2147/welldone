package org.whatever.io

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import kotlin.jvm.Throws

@Throws(NullPointerException::class)
fun readImageFile(file: File) = ImmutableImage.loader().fromFile(file).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!

@Throws(NullPointerException::class)
fun readImageUrl(url: URL): BufferedImage {
    val connection = url.openConnection()!!
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0",
    )
    val inputStream = connection.getInputStream()!!
    val image =
        ImmutableImage
            .loader()
            .fromStream(inputStream)
            .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!
            .clampTransparency()

    inputStream.close()
    return image
}

fun readGifUrl(url: URL): List<BufferedImage> {
    val connection = url.openConnection()!!
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0",
    )
    val inputStream = connection.getInputStream()!!
    val animatedGif = AnimatedGifReader.read(ImageSource.of(inputStream))!!
    inputStream.close()
    return animatedGif.frames.map { it.toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!! }
}

private fun BufferedImage.clampTransparency(): BufferedImage {
    for (y in 0..<this.height) {
        for (x in 0..<this.width) {
            val color = RGBColor.fromARGBInt(this.getRGB(x, y))
            this.setRGB(x, y, RGBColor(color.red, color.green, color.blue, if (color.alpha < 6) 0 else 255).toARGBInt())
        }
    }
    return this
}
