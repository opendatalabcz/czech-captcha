package cz.opendatalab.captcha

import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

object Utils {
    fun generateUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Get lowercase extension without the dot from filename.
     */
    fun getFileExtension(filename: String?): String {
        filename ?: return ""
        val index = filename.lastIndexOf('.')
        if (index > 0) {
            return filename.substring(index + 1).lowercase(Locale.getDefault())
        }
        return ""
    }

    fun getBytesFromInputStream(inputStream: InputStream): ByteArray {
        ByteArrayOutputStream().use { os ->
            inputStream.use { inputStream -> IOUtils.copy(inputStream, os) }
            return os.toByteArray()
        }
    }
}
