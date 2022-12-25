package cz.opendatalab.captcha.datamanagement.objectstorage

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Repository
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Repository
class ObjectRepository(
    private val urlRepo: UrlObjectRepository,
    private val fsRepo: FilesystemObjectRepository
){
    /**
     * InputStream should be closed manually after use.
     */
    fun getFile(path: String, repoType: ObjectRepositoryType): InputStream {
        return getRepo(repoType).getFile(path)
    }

    /**
     * InputStream with content is closed after save.
     * @returns path to the saved file
     */
    fun saveFile(content: InputStream, name: String, repoType: ObjectRepositoryType): String {
        return getRepo(repoType).saveFile(content, name)
    }

    fun removeFile(path: String, repoType: ObjectRepositoryType) {
        getRepo(repoType).removeFile(path)
    }

    private fun getRepo(repoType: ObjectRepositoryType): ObjectRepositoryInterface {
        return when(repoType) {
            ObjectRepositoryType.URL -> urlRepo
            ObjectRepositoryType.FILESYSTEM -> fsRepo
        }
    }
}

interface ObjectRepositoryInterface {
    fun getFile(path: String): InputStream
    fun saveFile(content: InputStream, name: String): String
    fun removeFile(path: String)
}

@Repository
class UrlObjectRepository : ObjectRepositoryInterface {
    override fun getFile(path: String): InputStream {
        val url = URL(path)
        return url.openStream()
    }

    override fun saveFile(content: InputStream, name: String): String {
        content.close()
        return name
    }

    override fun removeFile(path: String) {
        // do nothing
    }
}

@Repository
class FilesystemObjectRepository(
    private val properties: FilesystemRepositoryProperties
): ObjectRepositoryInterface {
    private val dataPath: Path = Paths.get(properties.dataPath)

    init {
        if ( ! Files.isDirectory(dataPath) ) {
            Files.createDirectories(dataPath)
        }
    }

    override fun getFile(path: String): InputStream {
        val filePath = findFileInBalancedDirs(dataPath, path) ?:
            throw FileNotFoundException("Cannot find file $path in ${properties.dataPath} or any subdirectories.")
        return Files.newInputStream(filePath)
    }

    override fun saveFile(content: InputStream, name: String): String {
        saveFileBalancingDirs(content, dataPath, name)
        return name
    }

    override fun removeFile(path: String) {
        val filePath = findFileInBalancedDirs(dataPath, path) ?: return
        Files.delete(filePath)
    }

    /**
     * Saves content of a file to the specified directory or a subdirectory so that no directory contains more than
     * properties.maxFilesPerDir files. Subdirectories are created from the first letter of the filename if needed.
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
        if ( filesInDir < properties.maxFilesPerDir || subFilename.startsWith('.') ) {
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
        content.use { Files.copy(it, finalPath, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun findFileInBalancedDirs(dir: Path, filename: String): Path? {
        val subDirName = filename[0].toString()
        val subFilename = filename.substring(1)
        val subDir = dir.resolve(subDirName)
        if ( Files.isDirectory(subDir) && !subFilename.startsWith('.')) {
            return findFileInBalancedDirs(subDir, subFilename)
        }
        val filePath = dir.resolve(filename)
        if ( Files.isRegularFile(filePath) ) {
            return filePath
        }
        return null
    }
}

@ConfigurationProperties(prefix = "datamanagement.filesystem")
data class FilesystemRepositoryProperties @ConstructorBinding constructor(
    val dataPath: String,
    val maxFilesPerDir: Int
) {}
