package cz.opendatalab.captcha.datamanagement

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroupRepository
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataRepository
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfoRepository
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.user.UserRepository
import io.mongock.driver.mongodb.springdata.v3.SpringDataMongoV3Driver
import io.mongock.runner.springboot.MongockSpringboot
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.util.FileSystemUtils
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
internal class ObjectControllerTest(
    private val context: ApplicationContext,
) {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var labelGroupRepository: LabelGroupRepository
    @Autowired
    private lateinit var objectMetadataRepository: ObjectMetadataRepository
    @Autowired
    private lateinit var objectStorageInfoRepository: ObjectStorageInfoRepository
    @Autowired
    private lateinit var siteConfigRepository: SiteConfigRepository
    @Autowired
    private lateinit var userRepository: UserRepository

    private val mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(mongo.connectionString))
        .build()
    private val mongoTemplate = MongoTemplate(MongoClients.create(mongoClientSettings), dbName)
    private val mongockRunner = MongockSpringboot.builder()
        .setDriver(SpringDataMongoV3Driver.withDefaultLock(mongoTemplate))
        .addMigrationScanPackage("cz.opendatalab.captcha.initialization.mongock")
        .setSpringContext(context)
        .buildRunner()

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val dbName = "captcha"

        private val mongo = MongoDBContainer("mongo:4.2")
            .withEnv("MONGO_INITDB_DATABASE", dbName)

        init {
            mongo.start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri", mongo::getConnectionString)
        }

        @AfterAll
        @JvmStatic
        private fun removeFilesCreatedDuringTests() {
            mongo.stop()
            try {
                FileSystemUtils.deleteRecursively(Paths.get("./czech-captcha"))
            } catch (e: Exception) {
                logger.warn("Could not delete directory with files created during tests.", e)
            }
        }
    }

    @BeforeEach
    fun clearMongoDB() {
        labelGroupRepository.deleteAll()
        objectMetadataRepository.deleteAll()
        objectStorageInfoRepository.deleteAll()
        siteConfigRepository.deleteAll()
        userRepository.deleteAll()
        mongoTemplate.dropCollection("mongockChangeLog")
        mongoTemplate.createCollection("mongockChangeLog")
        mongockRunner.execute()
    }

    @Test
    fun getDataObjects() {
        mvc.perform(
            get("/api/datamanagement/objects")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[{\"metadata\":{\"owner\":\"user\",\"objectType\":{\"format\":\"jpg\",\"type\":\"IMAGE\"},\"tags\":[],\"labels\":{},\"otherMetadata\":{}},\"storageInfo\":{\"id\":\"bfa25bc2-7350-4b99-99cc-8feaa8e1991b\",\"originalName\":\"677743874_168dcb5ed1_z.jpg\",\"path\":\"https://c5.staticflickr.com/2/1244/677743874_168dcb5ed1_z.jpg\",\"repositoryType\":\"URL\"}}]"))
    }

    @Test
    fun getAllObjectMetadata() {
        mvc.perform(
            get("/api/datamanagement/objects/metadata")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[{\"id\":\"bfa25bc2-7350-4b99-99cc-8feaa8e1991b\",\"owner\":\"user\",\"objectType\":{\"format\":\"jpg\",\"type\":\"IMAGE\"},\"tags\":[],\"labels\":{},\"otherMetadata\":{}}]"))
    }

    @Test
    fun addURLObject() {
        mvc.perform(
            post("/api/datamanagement/objects/url")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"metadata\":{\"knownLabels\":{\"all\":[\"dog\"]},\"tags\":[\"tag1\"]},\"url\":\"https://c5.staticflickr.com/2/1244/677743874_168dcb5ed1_z.jpg\"}\n")
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun addURLImage() {
        mvc.perform(
            post("/api/datamanagement/objects/image/url")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://c5.staticflickr.com/2/1244/677743874_168dcb5ed1_z.jpg\",\"metadata\":{\"knownLabels\":{\"all\":[\"dog\"]},\"tags\":[\"tag1\"]},\"objectDetection\":{\"objectDetectionParameters\":{\"wantedLabels\":{\"object_detection\":[\"person\",\"dog\"]},\"thresholdOneVote\":0.6,\"thresholdTwoVotes\":0.8},\"annotations\":null}}\n")
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun getLabelGroups() {
        mvc.perform(
            get("/api/datamanagement/objects/labelgroups")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("[{\"name\":\"object_detection\",\"labelRange\":[\"person\",\"bicycle\",\"car\",\"motorcycle\",\"airplane\",\"bus\",\"train\",\"truck\",\"boat\",\"traffic light\",\"fire hydrant\",\"stop sign\",\"parking meter\",\"bench\",\"bird\",\"cat\",\"dog\",\"horse\",\"sheep\",\"cow\",\"elephant\",\"bear\",\"zebra\",\"giraffe\",\"backpack\",\"umbrella\",\"handbag\",\"tie\",\"suitcase\",\"frisbee\",\"skis\",\"snowboard\",\"sports ball\",\"kite\",\"baseball bat\",\"baseball glove\",\"skateboard\",\"surfboard\",\"tennis racket\",\"bottle\",\"wine glass\",\"cup\",\"fork\",\"knife\",\"spoon\",\"bowl\",\"banana\",\"apple\",\"sandwich\",\"orange\",\"broccoli\",\"carrot\",\"hot dog\",\"pizza\",\"donut\",\"cake\",\"chair\",\"couch\",\"potted plant\",\"bed\",\"dining table\",\"toilet\",\"tv\",\"laptop\",\"mouse\",\"remote\",\"keyboard\",\"cell phone\",\"microwave\",\"oven\",\"toaster\",\"sink\",\"refrigerator\",\"book\",\"clock\",\"vase\",\"scissors\",\"teddy bear\",\"hair drier\",\"toothbrush\"],\"maxCardinality\":80},{\"name\":\"all\",\"maxCardinality\":2147483647}]"))
    }

    @Test
    fun getLabelGroup() {
        mvc.perform(
            get("/api/datamanagement/objects/labelgroups/all")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"name\":\"all\",\"maxCardinality\":2147483647}"))
    }

    @Test
    fun createLabelGroup() {
        mvc.perform(
            post("/api/datamanagement/objects/labelgroups")
                .with(httpBasic("user","user"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new-label-group\", \"maxCardinality\": 42}")
        )
            .andExpect(status().isCreated)
    }
}