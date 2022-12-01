package cz.opendatalab.captcha

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO

object TestImages {
    const val NAME_1 = "test_image_1.jpg"
    const val NAME_2 = "test_image_2.jpg"
    val BYTES_1 = Utils.getBytesFromInputStream(Thread.currentThread().contextClassLoader.getResourceAsStream(NAME_1)!!)
    val BYTES_2 = Utils.getBytesFromInputStream(Thread.currentThread().contextClassLoader.getResourceAsStream(NAME_2)!!)
    val IMAGE_1 = ImageIO.read(getInputStream1())!!
    val IMAGE_2 = ImageIO.read(getInputStream2())!!
    private val BASE64_BYTES_1 = String(Base64.getEncoder().encode(BYTES_1))
    private val BASE64_BYTES_2 = String(Base64.getEncoder().encode(BYTES_2))

    fun getInputStream1(): InputStream {
        return ByteArrayInputStream(BYTES_1)
    }

    fun getInputStream2(): InputStream {
        return ByteArrayInputStream(BYTES_2)
    }

    fun getBase64StringImage1(format: String): String {
        return "data:image/$format;base64,$BASE64_BYTES_1"
    }

    fun getBase64StringImage2(format: String): String {
        return "data:image/$format;base64,$BASE64_BYTES_2"
    }
}