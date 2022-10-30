package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.TestConfiguration
import cz.opendatalab.captcha.Utils
import cz.opendatalab.captcha.datamanagement.objectmetadata.TextObjectType
import io.mockk.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal class ObjectServiceTest {

    private val maxSize = 1024
    private val objectCatalogue: ObjectCatalogue = mockk()
    private val objectService = ObjectService(objectCatalogue, maxSize)

    private val user = "user"
    private val url = "url"
    private val objectStorageInfo = ObjectStorageInfo(uuid, url, user, url, ObjectRepositoryType.URL)

    @Test
    fun deleteFile() {
        every { objectCatalogue.findByIdOrNull(uuid) } returns objectStorageInfo
        every { objectCatalogue.deleteById(uuid) } returns Unit

        objectService.deleteFile(uuid)

        verify { objectCatalogue.findByIdOrNull(uuid) }
        verify { objectCatalogue.deleteById(uuid) }
        verify { FileRepository.removeFile(url, ObjectRepositoryType.URL) }
    }

    @Test
    fun saveURLFile() {
        every { objectCatalogue.insert(objectStorageInfo) } returns objectStorageInfo

        assertEquals(uuid, objectService.saveURLFile(user, url))

        verify { Utils.generateUniqueId() }
        verify { objectCatalogue.insert(objectStorageInfo) }
    }

    @Test
    fun getById() {
        val inputStream = ByteArrayInputStream("test".toByteArray())

        every { objectCatalogue.findByIdOrNull(uuid) } returns objectStorageInfo
        every { FileRepository.getFile(objectStorageInfo.path, objectStorageInfo.repositoryType) } returns
                inputStream

        assertEquals(inputStream, objectService.getById(uuid))

        verify { objectCatalogue.findByIdOrNull(uuid) }
        verify { FileRepository.getFile(objectStorageInfo.path, objectStorageInfo.repositoryType) }

        inputStream.close()
    }

    @Test
    fun `getById wrong id`() {
        every { objectCatalogue.findByIdOrNull(uuid) } returns null

        assertNull(objectService.getById(uuid))

        verify { objectCatalogue.findByIdOrNull(uuid) }
    }

    @Test
    fun getInfoByIdList() {
        val ids = listOf("id1", "id2")
        every { objectCatalogue.findAllById(ids) } returns emptyList()

        assertEquals(emptyList<ObjectStorageInfo>(), objectService.getInfoByIdList(ids))

        verify { objectCatalogue.findAllById(ids) }
    }

    @Test
    fun saveFile() {
        val filename = "file.txt"
        val file = MockMultipartFile(filename, filename, MediaType.TEXT_PLAIN_VALUE, "test_text".toByteArray())
        val newFilename = "$uuid.txt"
        val info = ObjectStorageInfo(uuid, filename, user, newFilename, ObjectRepositoryType.FILESYSTEM)

        every { objectCatalogue.insert(info) } returns info

        assertEquals(uuid, objectService.saveFile(user, file, TextObjectType))

        verify { FileRepository.saveFile(any(), newFilename, ObjectRepositoryType.FILESYSTEM)}
        verify { objectCatalogue.insert(info) }
    }

    @Test
    fun saveImageFile() {
        val image = Thread.currentThread().contextClassLoader
            .getResourceAsStream(TestConfiguration.TEST_IMAGE_1).use {
            ImageIO.read(it)
        }
        val imageFormat = "jpg"
        val newFilename = "$uuid.$imageFormat"
        val info = ObjectStorageInfo(uuid, TestConfiguration.TEST_IMAGE_1, user, newFilename, ObjectRepositoryType.FILESYSTEM)

        every { objectCatalogue.insert(info) } returns info

        assertEquals(uuid, objectService.saveImageFile(image, imageFormat, TestConfiguration.TEST_IMAGE_1, user))

        verify { FileRepository.saveFile(any(), newFilename, ObjectRepositoryType.FILESYSTEM)}
        verify { objectCatalogue.insert(info) }
    }

    companion object {
        private val uuid = "123e4567-e89b-12d3-a456-426614174000"

        @BeforeAll
        @JvmStatic
        fun mockObjects() {
            mockkObject(FileRepository)
            mockkObject(Utils)
            every { FileRepository.saveFile(any(), any(), ObjectRepositoryType.FILESYSTEM) } returns uuid
            every { Utils.generateUniqueId() } returns uuid
        }

        @AfterAll
        @JvmStatic
        fun unmockObjects() {
            unmockkAll()
        }
    }
}
