package org.whatever.images

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import io.ktor.http.*
import org.whatever.classExtensions.getPxlsStyleParams
import org.whatever.exceptions.LinkParameterMissingException
import org.whatever.io.readImageUrl
import org.whatever.logger
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLDecoder
import javax.imageio.ImageIO
import javax.naming.MalformedLinkException
import kotlin.io.encoding.Base64
import kotlin.system.exitProcess

class Detemplatizer(
    private val dataType: DataType,
) {
    companion object {
        enum class DataType {
            Base64,
            ImageLink,
        }

        fun getDetemplatizer(templateLink: String): Detemplatizer {
            logger.info("Get template type")
            return when (Url(templateLink).getPxlsStyleParams()["template"]?.startsWith("data")) {
                null -> throw LinkParameterMissingException()
                true -> Detemplatizer(DataType.Base64)
                false -> Detemplatizer(DataType.ImageLink)
            }
        }
    }

    fun detemplatize(pxlsLink: String): BufferedImage {
        val pxlsUrl = Url(pxlsLink)
        logger.info("Get image width")
        val params = pxlsUrl.getPxlsStyleParams()
        val imageWidth =
            (params["tw"] ?: throw LinkParameterMissingException()).toIntOrNull() ?: throw MalformedLinkException()
        return when (dataType) {
            DataType.Base64 -> {
                logger.info("Get template data (base64)")
                val imageData =
                    URLDecoder.decode(
                        URLDecoder.decode(
                            params["template"] ?: throw LinkParameterMissingException(),
                            charset("UTF-8")
                        )
                            .split(
                                "Bbase64,",
                            ).drop(1)
                            .joinToString("Bbase64,"),
                        charset("UTF-8")
                    )
                detemplatizeBase64(imageData, imageWidth)
            }

            DataType.ImageLink -> {
                logger.info("Get template data (link)")
                try {
                    val imageUrl =
                        URLDecoder.decode(
                            params["template"] ?: throw LinkParameterMissingException(),
                            charset("UTF-8")
                        )
                    detemplatizeUrl(imageUrl, imageWidth)
                } catch (e: Exception) {
                    logger.exception(e)
                    exitProcess(1)
                }
            }
        }
    }

    private fun detemplatizeBase64(
        imageData: String,
        imageWidth: Int,
    ): BufferedImage {
        try {
            val imageBytes = Base64.decode(imageData)
            val image =
                ImmutableImage
                    .fromAwt(ImageIO.read(ByteArrayInputStream(imageBytes)))
                    .toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!
            logger.info("Image params: ${image.width}, ${image.height}")
            if (imageWidth <= -1) {
                return image
            }
            return detemplatizeImage(image, imageWidth)
        } catch (e: Exception) {
            logger.exception(e)
            throw MalformedLinkException()
        }
    }

    private fun detemplatizeUrl(
        url: String,
        imageWidth: Int,
    ): BufferedImage {
        logger.info(url)
        val image =
            try {
                readImageUrl(URI(url).toURL())
            } catch (e: Exception) {
                logger.exception(e)
                exitProcess(-1)
            }
        logger.info("Got image with width ${image.width}")
        if (imageWidth <= -1) {
            return image
        }
        return detemplatizeImage(image, imageWidth)
    }

    private fun detemplatizeImage(
        image: BufferedImage,
        imageWidth: Int,
    ): BufferedImage {
        val pixelSize = image.width / imageWidth
        val outputImage = BufferedImage(image.width / pixelSize, image.height / pixelSize, BufferedImage.TYPE_INT_ARGB)
        for (x in 0..<outputImage.width) {
            for (y in 0..<outputImage.height) {
                val subsection = image.getSubimage(x * pixelSize, y * pixelSize, pixelSize, pixelSize)
                for (subX in 0..<subsection.width) {
                    var success = false
                    for (subY in 0..<subsection.height) {
                        if (RGBColor.fromARGBInt(subsection.getRGB(subX, subY)).alpha != 0) {
                            outputImage.setRGB(x, y, subsection.getRGB(subX, subY))
                            success = true
                            break
                        }
                    }
                    if (success) {
                        break
                    }
                }
            }
        }
        return outputImage
    }
}
