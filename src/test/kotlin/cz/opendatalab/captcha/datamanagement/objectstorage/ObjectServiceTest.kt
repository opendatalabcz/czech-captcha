package cz.opendatalab.captcha.datamanagement.objectstorage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

internal class ObjectServiceTest {

    private val maxSize = 1024
    private val objectCatalogue: ObjectCatalogue = mockk()
    private val objectService = ObjectService(objectCatalogue, maxSize)


    @Test
    fun deleteFile() {
        val id = "id"
        val info = ObjectStorageInfo(id, "user", "path", ObjectRepositoryType.URL)

        every { objectCatalogue.findByIdOrNull(id) } returns info
        every { objectCatalogue.deleteById(id) } returns Unit

        objectService.deleteFile(id)

        verify { objectCatalogue.deleteById(id) }
    }

    @Test
    fun saveURLFile() {
        val id = "id"
        val url = "url"
        val info = ObjectStorageInfo(id, "user", url, ObjectRepositoryType.URL)

        every { objectCatalogue.insert(any<ObjectStorageInfo>()) } returns info

        val resultId = objectService.saveURLFile(info.user, url)

        verify { objectCatalogue.insert(info.copy(id = resultId)) }
    }
}
