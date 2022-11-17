package cz.opendatalab.captcha.datamanagement.objectstorage

import org.apache.commons.configuration.PropertiesConfiguration
import org.springframework.http.HttpStatus
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
    fun saveFile(content: InputStream, name: String): String

    fun removeFile(path: String)

    companion object {
        fun getFile(path: String, repoType: ObjectRepositoryType): InputStream? {
            return getRepo(repoType).getFile(path)
        }

        fun saveFile(content: InputStream, name: String, repoType: ObjectRepositoryType): String {
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

    override fun saveFile(content: InputStream, name: String): String {
        return name
    }

    override fun removeFile(path: String) {
        // do nothing
    }
}

object FilesystemFileRepository : FileRepository {

    private const val DATA_PATH_PROPERTY = "datamanagement.filesystem.data-path"
    private const val MAX_FILES_IN_DIR_PROPERTY = "datamanagement.filesystem.max-files-per-dir"

    private val DATA_PATH: Path
    private val MAX_FILES_IN_DIR: Int

    init { // load data from application.properties - object is not managed by Spring
        val config = PropertiesConfiguration()
        config.load("application.properties")

        MAX_FILES_IN_DIR = config.getInteger(MAX_FILES_IN_DIR_PROPERTY, null)
            ?: throw IllegalStateException("$MAX_FILES_IN_DIR_PROPERTY not provided in application.properties")

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

    override fun saveFile(content: InputStream, name: String): String {
        saveFileBalancingDirs(content, DATA_PATH, name)
        return name
    }

    override fun removeFile(path: String) {
        val filePath = findFile(DATA_PATH, path) ?: return
        Files.delete(filePath)
    }

    /**
     * Saves content of a file to the specified directory or a subdirectory so that no directory contains more than
     * MAX_FILES_IN_DIR files. Subdirectories are created from the first letter of the filename if needed.
     */
    private fun saveFileBalancingDirs(content: InputStream, dir: Path, filename: String) {
        val subDirName = filename[0].toString()
        val subFilename = filename.substring(1)
        val subDir = dir.resolve(subDirName)
        if ( Files.isDirectory(subDir) ) {
            saveFileBalancingDirs(content, subDir, subFilename)
            return
        }

        val filesInDir = Files.newDirectoryStream(dir).use {
            it.count()
        }
        if ( filesInDir < MAX_FILES_IN_DIR || subFilename.startsWith('.') ) {
            saveFileToDir(content, dir, filename)
            return
        }

        Files.createDirectory(subDir)
        saveFileToDir(content, subDir, subFilename)
        Files.newDirectoryStream(dir).use {
            for (source in it) {
                val filenameToMove = source.fileName.toString()
                if (Files.isRegularFile(source) &&
                    filenameToMove.startsWith(subDirName) &&
                    !filenameToMove.substring(1).startsWith('.')) {
                    val destination = subDir.resolve(filenameToMove.substring(1))
                    Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun saveFileToDir(content: InputStream, path: Path, filename: String) {
        val finalPath = path.resolve(filename)
        Files.copy(content, finalPath, StandardCopyOption.REPLACE_EXISTING)
        content.close()
    }

    private fun findFile(dir: Path, filename: String): Path? {
        val subDirName = filename[0].toString()
        val subFilename = filename.substring(1)
        val subDir = dir.resolve(subDirName)
        if ( Files.isDirectory(subDir) && !subFilename.startsWith('.')) {
            return findFile(subDir, subFilename)
        }
        val filePath = dir.resolve(filename)
        if ( Files.isRegularFile(filePath) ) {
            return filePath
        }
        return null
    }
}
