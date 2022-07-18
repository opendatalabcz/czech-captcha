package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.Utils.getFileExtension
import org.apache.commons.configuration.PropertiesConfiguration
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

interface FileRepository {

    /**
     * InputStream should be closed manually after use.
     */
    fun getFile(path: String): InputStream?

    /**
     * @returns path
     */
    fun saveFile(content: MultipartFile, name: String): String

    fun removeFile(path: String)

    companion object {
        fun getFile(path: String, repoType: ObjectRepositoryType): InputStream? {
            return getRepo(repoType).getFile(path)
        }

        fun saveFile(content: MultipartFile, name: String, repoType: ObjectRepositoryType): String {
            return getRepo(repoType).saveFile(content, name)
        }

        fun removeFile(path: String, repoType: ObjectRepositoryType) {
            return getRepo(repoType).removeFile(path)
        }

        private fun getRepo(repoType: ObjectRepositoryType): FileRepository {
            return when(repoType) {
                ObjectRepositoryType.URL -> UrlFileRepository
                ObjectRepositoryType.FILESYSTEM -> FilesystemFileRepository
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
}

object FilesystemFileRepository : FileRepository {

    private const val DATA_PATH_PROPERTY = "datamanagement.filesystem.data-path"

    private val DATA_PATH: Path

    private const val MAX_FILES_IN_DIR = 10000

    init { // load DATA_PATH from application.properties - object is not managed by Spring
        val config = PropertiesConfiguration()
        config.load("application.properties")
        val pathStr = config.getString(DATA_PATH_PROPERTY)
            ?: throw IllegalStateException("$DATA_PATH_PROPERTY not provided in application.properties")
        DATA_PATH = Paths.get(pathStr)
        if ( ! Files.isDirectory(DATA_PATH) ) {
            Files.createDirectories(DATA_PATH)
        }
    }

    override fun getFile(path: String): InputStream? {
        val filePath = findFile(DATA_PATH, path) ?: return null
        return Files.newInputStream(filePath)
    }

    override fun saveFile(content: MultipartFile, name: String): String {
        val fileName = name + getFileExtension(content.originalFilename)
        saveFileBalancingDirs(content, DATA_PATH, fileName)
        return fileName
    }

    override fun removeFile(path: String) {
        val filePath = findFile(DATA_PATH, path) ?: return
        Files.delete(filePath)
    }

    /**
     * Saves content of a file to the specified directory or a subdirectory so that no directory contains more than
     * MAX_FILES_IN_DIR files. Subdirectories are created from the first letter of the filename if needed.
     */
    private fun saveFileBalancingDirs(content: MultipartFile, dir: Path, filename: String) {
        val subDirName = filename[0].toString()
        val subFilename = filename.substring(1)
        val subDir = dir.resolve(subDirName)
        if ( Files.isDirectory(subDir) ) {
            saveFileBalancingDirs(content, subDir, subFilename)
            return
        }

        val directoryStream = Files.newDirectoryStream(dir)
        val filesInDir = directoryStream.count()
        if ( filesInDir < MAX_FILES_IN_DIR ) {
            saveFileToDir(content, dir, filename)
            directoryStream.close()
            return
        }

        Files.createDirectory(subDir)
        saveFileToDir(content, subDir, subFilename)
        for (source in directoryStream) {
            if ( Files.isRegularFile(source) && source.fileName.startsWith(subDirName) ) {
                val destination = subDir.resolve(source.fileName.toString().substring(1))
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        directoryStream.close()
    }

    private fun saveFileToDir(content: MultipartFile, path: Path, filename: String) {
        val finalPath = path.resolve(filename)
        content.inputStream.use { Files.copy(it, finalPath, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun findFile(dir: Path, filename: String): Path? {
        val subDirName = filename[0].toString()
        val subFilename = filename.substring(1)
        val subDir = dir.resolve(subDirName)
        if ( Files.isDirectory(subDir) ) {
            return findFile(subDir, subFilename)
        }
        val filePath = dir.resolve(filename)
        if ( Files.isRegularFile(filePath) ) {
            return filePath
        }
        return null
    }
}
