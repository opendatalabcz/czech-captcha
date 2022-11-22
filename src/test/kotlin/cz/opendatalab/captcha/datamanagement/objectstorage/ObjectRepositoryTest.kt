package cz.opendatalab.captcha.datamanagement.objectstorage

import org.apache.commons.configuration.PropertiesConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

internal class ObjectRepositoryTest {
    private val DATA_PATH_PROPERTY = "datamanagement.filesystem.data-path"
    private val MAX_FILES_IN_DIR_PROPERTY = "datamanagement.filesystem.max-files-per-dir"

    private val DATA_PATH_STR: String
    private val MAX_FILES_IN_DIR: Int // tests should work for 5

    init { // load data from application.properties
        val config = PropertiesConfiguration()
        config.load("application.properties")

        MAX_FILES_IN_DIR = config.getInteger(MAX_FILES_IN_DIR_PROPERTY, null)
            ?: throw IllegalStateException("$MAX_FILES_IN_DIR_PROPERTY not provided in application.properties")

        DATA_PATH_STR = config.getString(DATA_PATH_PROPERTY)
            ?: throw IllegalStateException("$DATA_PATH_PROPERTY not provided in application.properties")
        val path = Paths.get(DATA_PATH_STR)
        if ( ! Files.isDirectory(path) ) {
            Files.createDirectories(path)
        }
    }
    private val testString = "test"

    private fun getInputStream(): InputStream {
        return ByteArrayInputStream(testString.toByteArray())
    }

    @AfterEach
    private fun deleteTestDirsAndFiles() {
        FileSystemUtils.deleteRecursively(Paths.get(DATA_PATH_STR))
    }

    @Test
    fun getFile_url_successful() {
        val url = Path.of("src/test/resources/test_image_1.jpg").toUri().toURL().toString()
        assertDoesNotThrow {
            val result = ObjectRepository.getFile(url, ObjectRepositoryType.URL)
            result.close()
        }
    }

    @Test
    fun getFile_invalidUrl_shouldThrow() {
        val url = "http://www.test-page.com/no-file-here.txt"
        assertThrows(FileNotFoundException::class.java) {
            val result = ObjectRepository.getFile(url, ObjectRepositoryType.URL)
            result.close()
        }
    }

    @Test
    fun saveFile_url_successful() {
        val name = "testname"
        val content = getInputStream()
        assertEquals(name, ObjectRepository.saveFile(content, name, ObjectRepositoryType.URL))
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_withOneFile_successful() {
        val content = getInputStream()
        val pathString = ObjectRepository.saveFile(content, "test_file.txt", ObjectRepositoryType.FILESYSTEM)
        val path = Paths.get(DATA_PATH_STR, pathString)
        assertTrue(path.isRegularFile())

        testGetFile(pathString)

        ObjectRepository.removeFile(pathString, ObjectRepositoryType.FILESYSTEM)
        assertFalse(Files.exists(path))
    }

    private fun testGetFile(pathString: String) {
        val inputStream = ObjectRepository.getFile(pathString, ObjectRepositoryType.FILESYSTEM)
        val fileContent = String(inputStream.readAllBytes())
        assertEquals(fileContent, testString)
        inputStream.close()
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_multipleFilesWithMoreLetters_shouldCreateSubdirs() {
        val numberOfFiles = MAX_FILES_IN_DIR + 2
        val pathStrings = mutableListOf<String>()
        val paths = mutableListOf<Path>()
        for (i in 1..numberOfFiles) {
            val content = getInputStream()
            val pathString = ObjectRepository.saveFile(content, "${i}0.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path =
                if (i > MAX_FILES_IN_DIR) {
                    Paths.get(DATA_PATH_STR, pathString[0].toString(), pathString.substring(1))
                } else {
                    Paths.get(DATA_PATH_STR, pathString)
                }
            paths.add(path)
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGetFile(pathString)
        }
        for (i in 0 until numberOfFiles) {
            ObjectRepository.removeFile(pathStrings[i], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(paths[i]))
        }
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_multipleFilesWithOneLetter_shouldNotCreateSubdirs() {
        val numberOfFiles = MAX_FILES_IN_DIR + 2
        val pathStrings = mutableListOf<String>()
        val paths = mutableListOf<Path>()
        for (i in 1..numberOfFiles) {
            val content = getInputStream()
            val pathString = ObjectRepository.saveFile(content, "${i}.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path = Paths.get(DATA_PATH_STR, pathString)
            paths.add(path)
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGetFile(pathString)
        }
        for (i in 1..numberOfFiles) {
            ObjectRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(paths[i - 1]))
        }
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_multipleFilesWithSamePrefix_shouldCreateSubdirAndMoveAllFilesWithPrefix() {
        val expectedPaths = mutableListOf<Path>()
        val numberOfFiles = MAX_FILES_IN_DIR + 2
        val pathStrings = mutableListOf<String>()
        for (i in 1..numberOfFiles) {
            val content = getInputStream()
            val pathString = ObjectRepository.saveFile(content, "00${i}.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path =
                if (i > MAX_FILES_IN_DIR + 1) {
                    Paths.get(DATA_PATH_STR, pathString[0].toString(), pathString[1].toString(), pathString.substring(2))
                } else if (i > MAX_FILES_IN_DIR) {
                    Paths.get(DATA_PATH_STR, pathString[0].toString(), pathString.substring(1))
                } else {
                    Paths.get(DATA_PATH_STR, pathString)
                }
            assertTrue(path.isRegularFile(), "$path is not regular file")
            expectedPaths.add(Paths.get(DATA_PATH_STR, "0", "0", "$i.txt"))
        }
        for (path in expectedPaths) {
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGetFile(pathString)
        }
        for (i in 1..numberOfFiles) {
            ObjectRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(expectedPaths[i - 1]))
        }
    }
}