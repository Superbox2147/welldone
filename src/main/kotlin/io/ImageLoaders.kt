package org.whatever.io

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import dev.matrixlab.webp4j.WebPCodec
import dev.matrixlab.webp4j.gif.GifToWebPConfig
import dev.matrixlab.webp4j.internal.NativeWebP
import dev.matrixlab.webp4j.model.WebPBitstreamFeatures
import org.whatever.logger
import org.whatever.types.ImageType
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import kotlin.jvm.Throws

@Throws(NullPointerException::class)
fun readImageFile(file: File) = ImmutableImage.loader().fromFile(file).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!

@Throws(NullPointerException::class)
fun readImageUrl(url: URL): BufferedImage = decodeImage(readBytes(url))

fun decodeGif(bytes: ByteArray): List<BufferedImage> {
    val encoderConfig = GifToWebPConfig.createLosslessConfig().apply {
        isMultiThreaded = true
        loopCount = 0
    }

    val animatedGif = WebPCodec.encodeGifToWebP(bytes, encoderConfig)

    val result = WebPCodec.decodeAnimatedWebP(animatedGif)

    return result.frames.map { it.image }
}

fun decodeWebP(bytes: ByteArray): List<BufferedImage> {
    val features = WebPBitstreamFeatures()
    NativeWebP.getFeatures(bytes, bytes.size, features)

    return if (features.isHasAnimation) {
        WebPCodec.decodeAnimatedWebP(bytes).frames.map { it.image }
    } else {
        listOf(WebPCodec.decodeImage(bytes))
    }
}

fun decodeImage(bytes: ByteArray): BufferedImage =
    ImmutableImage
        .loader()
        .fromBytes(bytes)
        .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!
        .clampTransparency()

fun loadAny(url: URL): List<BufferedImage> {
    logger.info("Got url $url")

    val bytes = readBytes(url)

    return when (determineTypeFromMagic(bytes)) {
        ImageType.Image -> listOf(decodeImage(bytes))
        ImageType.Gif -> decodeGif(bytes)
        ImageType.WebP -> decodeWebP(bytes)
    }
}

private fun readBytes(url: URL): ByteArray {
    logger.info("Reading from $url")

    val connection = url.openConnection()!!
    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0",
    )
    val inputStream = connection.getInputStream()!!
    val bytes = inputStream.readAllBytes()
    inputStream.close()
    return bytes
}

private fun determineTypeFromMagic(bytes: ByteArray): ImageType {
    val pngMagic = listOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    val gifMagicA = listOf(0x47, 0x49, 0x46, 0x38, 0x37, 0x61)
    val gifMagicB = listOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
    val webpMagic = listOf(0x52, 0x49, 0x46, 0x46, 0xFF, 0xFF, 0xFF, 0xFF, 0x57, 0x45, 0x42, 0x50)

    if (compareByteArrays(pngMagic, bytes)) return ImageType.Image
    if (compareByteArrays(gifMagicA, bytes)) return ImageType.Gif
    if (compareByteArrays(gifMagicB, bytes)) return ImageType.Gif
    if (compareByteArrays(webpMagic, bytes)) return ImageType.WebP

    return ImageType.Image
}

private fun compareByteArrays(reference: List<Int>, bytes: ByteArray): Boolean {
    for ((index, intByte) in reference.withIndex()) {
        if (intByte != 0xFF && intByte.toUInt().toUByte() != bytes[index].toUByte()) return false
    }

    return true
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