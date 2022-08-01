package cz.opendatalab.captcha.datamanagement.objectstorage

import org.apache.commons.configuration.PropertiesConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import java.io.ByteArrayInputStream
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

    @Test
    fun getUrlFile() {
        val url = Path.of("src/test/resources/test_image.jpg").toUri().toURL().toString()
        val result = FileRepository.getFile(url, ObjectRepositoryType.URL)
        assertNotNull(result)
        result?.close()
    }

    @Test
    fun saveUrlFile() {
        val name = "testname"
        val content = ByteArrayInputStream(testString.toByteArray())
        val resName = FileRepository.saveFile(content, name, ObjectRepositoryType.URL)
        assertEquals(resName, name)
    }

    @Test
    fun saveGetRemoveFile() {
        val content = ByteArrayInputStream(testString.toByteArray())
        val pathString = FileRepository.saveFile(content, "test_file.txt", ObjectRepositoryType.FILESYSTEM)
        val path = Paths.get(DATA_PATH_STR, pathString)
        assertTrue(path.isRegularFile())

        testGet(pathString)

        FileRepository.removeFile(pathString, ObjectRepositoryType.FILESYSTEM)
        assertFalse(Files.exists(path))
    }

    private fun testGet(pathString: String) {
        val inputStream = FileRepository.getFile(pathString, ObjectRepositoryType.FILESYSTEM)
        assertNotNull(inputStream)
        val fileContent = String(inputStream?.readAllBytes() ?: return)
        assertEquals(fileContent, testString)
        inputStream.close()
    }

    @Test
    fun saveGetRemoveMultipleFilesWithMoreLetters() {
        val numberOfFiles = MAX_FILES_IN_DIR + 2
        val pathStrings = mutableListOf<String>()
        val paths = mutableListOf<Path>()
        for (i in 1..numberOfFiles) {
            val content = ByteArrayInputStream(testString.toByteArray())
            val pathString = FileRepository.saveFile(content, "${i}0.txt", ObjectRepositoryType.FILESYSTEM)
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
            testGet(pathString)
        }
        for (i in 1..numberOfFiles) {
            FileRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(paths[i - 1]))
        }
    }

    @Test
    fun saveGetRemoveMultipleFilesWithOneLetter() {
        val numberOfFiles = MAX_FILES_IN_DIR + 2
        val pathStrings = mutableListOf<String>()
        val paths = mutableListOf<Path>()
        for (i in 1..numberOfFiles) {
            val content = ByteArrayInputStream(testString.toByteArray())
            val pathString = FileRepository.saveFile(content, "${i}.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path = Paths.get(DATA_PATH_STR, pathString)
            paths.add(path)
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGet(pathString)
        }
        for (i in 1..numberOfFiles) {
            FileRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(paths[i - 1]))
        }
    }

    @Test
    fun saveGetRemoveMultipleFilesMovingFilesToNewFolder() {
        val expectedPaths = mutableListOf<Path>()
        val numberOfFiles = MAX_FILES_IN_DIR + 2
        val pathStrings = mutableListOf<String>()
        for (i in 1..numberOfFiles) {
            val content = ByteArrayInputStream(testString.toByteArray())
            val pathString = FileRepository.saveFile(content, "00${i}.txt", ObjectRepositoryType.FILESYSTEM)
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
            testGet(pathString)
        }
        for (i in 1..numberOfFiles) {
            FileRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(expectedPaths[i - 1]))
        }
    }

    @AfterEach
    private fun deleteTestPath() {
        FileSystemUtils.deleteRecursively(Paths.get(DATA_PATH_STR))
    }
}