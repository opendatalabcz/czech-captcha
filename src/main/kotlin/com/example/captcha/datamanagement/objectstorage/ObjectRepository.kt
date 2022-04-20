package com.example.captcha.datamanagement.objectstorage

import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream
import java.net.URL

interface FileRepository {

    // todo content won't be string
    fun getFile(path: String): InputStream

    /**
     * returns path
     */
    fun saveFile(content: MultipartFile, name: String):String

    fun removeFile(path: String)

//    fun getPath(name: String): String

    companion object {
        fun getFile(path: String, repoType: ObjectRepositoryType): InputStream {
            return getRepo(repoType).getFile(path)
        }

        fun saveFile(content: MultipartFile, name: String, repoType: ObjectRepositoryType):String {
            return getRepo(repoType).saveFile(content, name)
        }

        fun removeFile(path: String, repoType: ObjectRepositoryType) {
            return getRepo(repoType).removeFile(path)
        }

//        fun getPath(name: String, repoType: FileRepositoryType): String {
//            return getRepo(repoType).getFile(name)
//        }

        private fun getRepo(repoType: ObjectRepositoryType): FileRepository {
            return when(repoType) {
                ObjectRepositoryType.URL -> UrlFileRepository
                else -> throw ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented")
            }
        }
    }
}


object UrlFileRepository : FileRepository {
    override fun getFile(path: String): InputStream {
        val url = URL(path)
        return url.openStream()
    }

    override fun saveFile(content: MultipartFile, name: String): String {
        return name
    }

    override fun removeFile(path: String) {
        // do nothing
    }


//    override fun getPath(name: String): String {
//        return name
//    }
}
