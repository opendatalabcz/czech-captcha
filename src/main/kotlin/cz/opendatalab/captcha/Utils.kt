package cz.opendatalab.captcha

import java.util.*

object Utils {
    fun generateUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    fun <T> selectRandom(list: List<T>, count: Int): List<T> {
        return list.shuffled().take(count)
    }

    /**
     * Get extension without the dot from filename.
     */
    fun getFileExtension(filename: String?): String {
        filename ?: return ""
        val index = filename.lastIndexOf('.')
        if (index > 0) {
            return filename.substring(index + 1)
        }
        return ""
    }
}
