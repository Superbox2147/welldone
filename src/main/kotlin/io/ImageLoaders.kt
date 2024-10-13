package org.whatever.io

import com.sksamuel.scrimage.ImmutableImage
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import kotlin.jvm.Throws

@Throws(NullPointerException::class)
fun readImageFile(file: File) = ImmutableImage.fromAwt(ImageIO.read(file)).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!

@Throws(NullPointerException::class)
fun readImageUrl(url: URL) = ImmutableImage.fromAwt(ImageIO.read(url)).toNewBufferedImage(BufferedImage.TYPE_INT_ARGB)!!
