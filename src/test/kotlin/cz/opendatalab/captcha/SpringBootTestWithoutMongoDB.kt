package cz.opendatalab.captcha

import cz.opendatalab.captcha.datamanagement.objectdetection.ObjectDetectorKotlinDL
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupRepository
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataRepository
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfoRepository
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.user.UserRepository
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@Import(RepositoriesTestConfig::class)
@ActiveProfiles("test")
annotation class SpringBootTestWithoutMongoDB

@TestConfiguration
class RepositoriesTestConfig {
    @MockBean lateinit var objectMetadataRepository: ObjectMetadataRepository
    @MockBean lateinit var objectStorageInfoRepository: ObjectStorageInfoRepository
    @MockBean lateinit var siteConfigRepository: SiteConfigRepository
    @MockBean lateinit var userRepository: UserRepository
    @MockBean lateinit var labelGroupRepository: LabelGroupRepository
    @MockBean lateinit var objectDetectorKotlinDL: ObjectDetectorKotlinDL
}