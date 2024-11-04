package org.whatever.io

import com.sksamuel.scrimage.ImmutableImage
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import kotlin.jvm.Throws

@Throws(NullPointerException::class)
fun readImageFile(file: File) = ImmutableImage.loader().fromFile(file).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!

@Throws(NullPointerException::class)
fun readImageUrl(url: URL): BufferedImage {
    val connection = url.openConnection()
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:37.0) Gecko/20100101 Firefox/37.0",
    )
    val inputStream = connection.getInputStream()
    val image = ImmutableImage.fromAwt(ImageIO.read(inputStream)).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!
    inputStream.close()
    return image
}
