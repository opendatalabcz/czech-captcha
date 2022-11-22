package cz.opendatalab.captcha

import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

object TestImages {
    const val NAME_1 = "test_image_1.jpg"
    const val NAME_2 = "test_image_2.jpg"
    val BYTES_1 = getBytes(Thread.currentThread().contextClassLoader.getResourceAsStream(NAME_1)!!)
    val BYTES_2 = getBytes(Thread.currentThread().contextClassLoader.getResourceAsStream(NAME_2)!!)
    val IMAGE_1 = ImageIO.read(getInputStream1())!!
    val IMAGE_2 = ImageIO.read(getInputStream2())!!

    fun getInputStream1(): InputStream {
        return ByteArrayInputStream(BYTES_1)
    }

    fun getInputStream2(): InputStream {
        return ByteArrayInputStream(BYTES_2)
    }

    private fun getBytes(inputStream: InputStream): ByteArray {
        ByteArrayOutputStream().use { os ->
            inputStream.use { inSt -> IOUtils.copy(inSt, os) }
            return os.toByteArray()
        }
    }
}