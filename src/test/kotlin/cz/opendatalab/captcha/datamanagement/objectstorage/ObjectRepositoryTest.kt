package cz.opendatalab.captcha.datamanagement.objectstorage

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
    private val dataPath = "test-save"
    private val maxFilesPerDir = 5
    private val properties = FilesystemRepositoryProperties(dataPath, maxFilesPerDir)
    private val objectRepository = ObjectRepository(
        UrlObjectRepository(),
        FilesystemObjectRepository(properties)
    )
    private val testString = "test"

    private fun getInputStream(): InputStream {
        return ByteArrayInputStream(testString.toByteArray())
    }

    @AfterEach
    private fun deleteTestDirsAndFiles() {
        FileSystemUtils.deleteRecursively(Paths.get(properties.dataPath))
    }

    @Test
    fun getFile_url_successful() {
        val url = Path.of("src/test/resources/test_image_1.jpg").toUri().toURL().toString()
        assertDoesNotThrow {
            val result = objectRepository.getFile(url, ObjectRepositoryType.URL)
            result.close()
        }
    }

    @Test
    fun getFile_invalidUrl_shouldThrow() {
        val url = "http://www.test-page.com/no-file-here.txt"
        assertThrows(FileNotFoundException::class.java) {
            val result = objectRepository.getFile(url, ObjectRepositoryType.URL)
            result.close()
        }
    }

    @Test
    fun saveFile_url_successful() {
        val name = "testname"
        val content = getInputStream()
        assertEquals(name, objectRepository.saveFile(content, name, ObjectRepositoryType.URL))
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_withOneFile_successful() {
        val content = getInputStream()
        val pathString = objectRepository.saveFile(content, "test_file.txt", ObjectRepositoryType.FILESYSTEM)
        val path = Paths.get(dataPath, pathString)
        assertTrue(path.isRegularFile())

        testGetFile(pathString)

        objectRepository.removeFile(pathString, ObjectRepositoryType.FILESYSTEM)
        assertFalse(Files.exists(path))
    }

    private fun testGetFile(pathString: String) {
        val inputStream = objectRepository.getFile(pathString, ObjectRepositoryType.FILESYSTEM)
        val fileContent = String(inputStream.readAllBytes())
        assertEquals(fileContent, testString)
        inputStream.close()
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_multipleFilesWithMoreLetters_shouldCreateSubdirs() {
        val numberOfFiles = maxFilesPerDir + 2
        val pathStrings = mutableListOf<String>()
        val paths = mutableListOf<Path>()
        for (i in 1..numberOfFiles) {
            val content = getInputStream()
            val pathString = objectRepository.saveFile(content, "${i}0.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path =
                if (i > maxFilesPerDir) {
                    Paths.get(dataPath, pathString[0].toString(), pathString.substring(1))
                } else {
                    Paths.get(dataPath, pathString)
                }
            paths.add(path)
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGetFile(pathString)
        }
        for (i in 0 until numberOfFiles) {
            objectRepository.removeFile(pathStrings[i], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(paths[i]))
        }
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_multipleFilesWithOneLetter_shouldNotCreateSubdirs() {
        val numberOfFiles = maxFilesPerDir + 2
        val pathStrings = mutableListOf<String>()
        val paths = mutableListOf<Path>()
        for (i in 1..numberOfFiles) {
            val content = getInputStream()
            val pathString = objectRepository.saveFile(content, "${i}.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path = Paths.get(dataPath, pathString)
            paths.add(path)
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGetFile(pathString)
        }
        for (i in 1..numberOfFiles) {
            objectRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(paths[i - 1]))
        }
    }

    @Test
    fun saveFileThenGetFileThenRemoveFile_multipleFilesWithSamePrefix_shouldCreateSubdirAndMoveAllFilesWithPrefix() {
        val expectedPaths = mutableListOf<Path>()
        val numberOfFiles = maxFilesPerDir + 2
        val pathStrings = mutableListOf<String>()
        for (i in 1..numberOfFiles) {
            val content = getInputStream()
            val pathString = objectRepository.saveFile(content, "00${i}.txt", ObjectRepositoryType.FILESYSTEM)
            pathStrings.add(pathString)
            val path =
                if (i > maxFilesPerDir + 1) {
                    Paths.get(dataPath, pathString[0].toString(), pathString[1].toString(), pathString.substring(2))
                } else if (i > maxFilesPerDir) {
                    Paths.get(dataPath, pathString[0].toString(), pathString.substring(1))
                } else {
                    Paths.get(dataPath, pathString)
                }
            assertTrue(path.isRegularFile(), "$path is not regular file")
            expectedPaths.add(Paths.get(dataPath, "0", "0", "$i.txt"))
        }
        for (path in expectedPaths) {
            assertTrue(path.isRegularFile())
        }
        for (pathString in pathStrings) {
            testGetFile(pathString)
        }
        for (i in 1..numberOfFiles) {
            objectRepository.removeFile(pathStrings[i - 1], ObjectRepositoryType.FILESYSTEM)
            assertFalse(Files.exists(expectedPaths[i - 1]))
        }
    }
}