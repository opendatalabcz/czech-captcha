package cz.opendatalab.captcha

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
}
