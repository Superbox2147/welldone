package org.whatever.io

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import kotlin.jvm.Throws

@Throws(NullPointerException::class)
fun readImageFile(file: File) = ImmutableImage.loader().fromFile(file).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!

@Throws(NullPointerException::class)
fun readImageUrl(url: URL): BufferedImage {
    // println(url.toString())
    val connection = url.openConnection()
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:37.0) Gecko/20100101 Firefox/37.0",
    )
    val inputStream = connection.getInputStream()
    val image =
        ImmutableImage
            .loader()
            .fromStream(inputStream)
            .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!
            .clampTransparency()

    inputStream.close()
    return image
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
