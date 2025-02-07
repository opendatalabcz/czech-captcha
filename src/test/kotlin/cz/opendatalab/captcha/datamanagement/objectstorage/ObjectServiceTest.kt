package cz.opendatalab.captcha.datamanagement.objectstorage

import cz.opendatalab.captcha.TestImages
import cz.opendatalab.captcha.Utils
import cz.opendatalab.captcha.datamanagement.ImageUtils
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.repository.findByIdOrNull
import java.io.ByteArrayInputStream
import java.io.InputStream

internal class ObjectServiceTest {

    private val maxSize = 1024
    private val objectStorageInfoRepository: ObjectStorageInfoRepository = mockk()
    private val objectRepository: ObjectRepository = mockk()
    private val objectService = ObjectService(objectStorageInfoRepository, objectRepository, maxSize)

    private val url = "http://some.website.com/hmg-prod.s3.amazonaws.com/images/image-for-service.test.jpg?crop=1.00xw:0.669xh;0,0.190xh&resize=640:*"
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"
    private val objectStorageInfoUrl = ObjectStorageInfo(uuid, "image-for-service.test.jpg", url, ObjectRepositoryType.URL)

    @BeforeEach
    fun mockObjects() {
        mockkObject(Utils)
        mockkObject(ImageUtils)
    }

    @AfterEach
    fun unmockObjects() {
        unmockkAll()
    }

    private fun getInputStream(): InputStream {
        return ByteArrayInputStream("test-content".toByteArray())
    }

    @Test
    fun getObjectById_validId_successful() {
        val content = getInputStream()

        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns objectStorageInfoUrl
        every { objectRepository.getFile(objectStorageInfoUrl.path, objectStorageInfoUrl.repositoryType) } returns
                content

        assertEquals(content, objectService.getObjectById(uuid))

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
        verify { objectRepository.getFile(objectStorageInfoUrl.path, objectStorageInfoUrl.repositoryType) }

        content.close()
    }

    @Test
    fun getObjectById_wrongId_shouldThrow() {
        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns null

        assertThrows(IllegalArgumentException::class.java) { objectService.getObjectById(uuid) }

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
    }

    @Test
    fun getImageById_validImage_successful() {
        val content = getInputStream()

        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns objectStorageInfoUrl
        every { objectRepository.getFile(objectStorageInfoUrl.path, objectStorageInfoUrl.repositoryType) } returns
                content
        every { ImageUtils.getImageFromInputStream(content) } returns TestImages.IMAGE_1

        assertEquals(TestImages.IMAGE_1, objectService.getImageById(uuid))

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
        verify { objectRepository.getFile(objectStorageInfoUrl.path, objectStorageInfoUrl.repositoryType) }
        verify { ImageUtils.getImageFromInputStream(content) }

        content.close()
    }

    @Test
    fun getInfoById_validId_successful() {
        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns objectStorageInfoUrl

        assertEquals(objectStorageInfoUrl, objectService.getInfoById(uuid))

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
    }

    @Test
    fun getInfoById_wrongId_shouldThrow() {
        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns null

        assertThrows(IllegalArgumentException::class.java) { objectService.getInfoById(uuid) }

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
    }

    @Test
    fun getInfoByIdList() {
        val ids = listOf("id1", "id2")
        every { objectStorageInfoRepository.findAllById(ids) } returns emptyList()

        assertEquals(0, objectService.getInfoByIdList(ids).size)

        verify { objectStorageInfoRepository.findAllById(ids) }
    }

    @Test
    fun saveUrlObject() {
        every { objectStorageInfoRepository.insert(objectStorageInfoUrl) } returns objectStorageInfoUrl
        every { Utils.generateUniqueId() } returns uuid

        assertEquals(objectStorageInfoUrl, objectService.saveUrlObject(url))

        verify { Utils.generateUniqueId() }
        verify { objectStorageInfoRepository.insert(objectStorageInfoUrl) }
    }

    @Test
    fun saveFileObject_textFile_successful() {
        val format = "txt"
        val originalFilename = "original-file.test.$format"
        val content = getInputStream()
        val filename = "$uuid.$format"
        val info = ObjectStorageInfo(uuid, originalFilename, filename, ObjectRepositoryType.FILESYSTEM)

        every { Utils.generateUniqueId() } returns uuid
        every { objectRepository.saveFile(content, filename, ObjectRepositoryType.FILESYSTEM) } returns filename
        every { objectStorageInfoRepository.insert(info) } returns info

        assertEquals(info, objectService.saveFileObject(content, originalFilename))

        verify { Utils.generateUniqueId() }
        verify { objectRepository.saveFile(content, filename, ObjectRepositoryType.FILESYSTEM) }
        verify { objectStorageInfoRepository.insert(info) }

        content.close()
    }

    @Test
    fun saveFileObject_imageFile_shouldResizeImage() {
        val format = "jpg"
        val originalFilename = "original-image.test.$format"
        val content1 = TestImages.getInputStream1()
        val content2 = TestImages.getInputStream2()
        val filename = "$uuid.$format"
        val info = ObjectStorageInfo(uuid, originalFilename, filename, ObjectRepositoryType.FILESYSTEM)

        every { Utils.generateUniqueId() } returns uuid
        every { ImageUtils.resizeImageFromInputStreamToMaxSize(content1, any(), format) } returns content2
        every { objectRepository.saveFile(content2, filename, ObjectRepositoryType.FILESYSTEM) } returns filename
        every { objectStorageInfoRepository.insert(info) } returns info

        assertEquals(info, objectService.saveFileObject(content1, originalFilename))

        verify { Utils.generateUniqueId() }
        verify { ImageUtils.resizeImageFromInputStreamToMaxSize(content1, any(), format) }
        verify { objectRepository.saveFile(content2, filename, ObjectRepositoryType.FILESYSTEM) }
        verify { objectStorageInfoRepository.insert(info) }

        content1.close()
        content2.close()
    }

    @Test
    fun deleteObject_withExistingObject_shouldDeleteItInObjectRepository() {
        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns objectStorageInfoUrl
        every { objectRepository.removeFile(url, ObjectRepositoryType.URL) } returns Unit
        every { objectStorageInfoRepository.deleteById(uuid) } returns Unit

        objectService.deleteObject(uuid)

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
        verify { objectRepository.removeFile(url, ObjectRepositoryType.URL) }
        verify { objectStorageInfoRepository.deleteById(uuid) }
    }

    @Test
    fun deleteObject_withNonExistingObject_shouldDoNothing() {
        every { objectStorageInfoRepository.findByIdOrNull(uuid) } returns null

        objectService.deleteObject(uuid)

        verify { objectStorageInfoRepository.findByIdOrNull(uuid) }
        verify(exactly = 0) { objectRepository.removeFile(any(), any()) }
        verify(exactly = 0) { objectStorageInfoRepository.deleteById(any()) }
    }
}
