package org.whatever.io

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import dev.matrixlab.webp4j.WebPCodec
import dev.matrixlab.webp4j.gif.GifToWebPConfig
import org.whatever.types.ImageType
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

    val encoderConfig = GifToWebPConfig.createLosslessConfig().apply {
        isMultiThreaded = true
        loopCount = 0
    }

    val animatedGif = WebPCodec.encodeGifToWebP(inputStream.readAllBytes(), encoderConfig)
    inputStream.close()

    val result = WebPCodec.decodeAnimatedWebP(animatedGif)

    return result.frames.map { it.image }
}

fun readWebPUrl(url: URL): List<BufferedImage> {
    val connection = url.openConnection()!!
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0",
    )
    val inputStream = connection.getInputStream()!!
    val webPBytes = inputStream.readAllBytes()
    inputStream.close()

    return WebPCodec.decodeAnimatedWebP(webPBytes).frames.map { it.image }
}

fun loadAny(url: URL, mediaType: String): List<BufferedImage> {
    return when (decodeMediaType(mediaType)) {
        ImageType.Image -> listOf(readImageUrl(url))
        ImageType.Gif -> readGifUrl(url)
        ImageType.WebP -> readWebPUrl(url)
    }
}

private fun decodeMediaType(mediaType: String): ImageType {
    return when (mediaType) {
        "image/gif" -> ImageType.Gif
        "image/webp" -> ImageType.WebP
        else -> ImageType.Image
    }
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