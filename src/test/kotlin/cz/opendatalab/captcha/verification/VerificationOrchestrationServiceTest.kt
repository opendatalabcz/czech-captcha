package cz.opendatalab.captcha.verification

import cz.opendatalab.captcha.SpringBootTestWithoutMongoDB
import cz.opendatalab.captcha.siteconfig.SiteConfig
import cz.opendatalab.captcha.siteconfig.SiteConfigRepository
import cz.opendatalab.captcha.siteconfig.TaskConfig
import cz.opendatalab.captcha.task.templates.EmptyGenerationConfig
import cz.opendatalab.captcha.verification.dto.VerificationDTO
import cz.opendatalab.captcha.verification.entities.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@SpringBootTestWithoutMongoDB
internal class VerificationOrchestrationServiceTest(
    @Autowired private val verService: VerificationOrchestrationService,
    @Autowired private val taskRepository: KeyValueStore<Task>,
    @Autowired private val tokenRepository: KeyValueStore<Token>,
    @Autowired private val siteConfigRepo: SiteConfigRepository
) {

    @Test
    fun generateTask() {
        val siteKey = "siteKey"
        val taskConfig = TaskConfig("Equation", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, "secretKey", "user", "configName", taskConfig)

        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        val taskDTO = verService.generateTask(siteKey)

        assertThat(taskDTO.taskType).isEqualTo(taskConfig.taskType)
        assertThat(taskDTO.expiration.isAfter(Instant.now())).isTrue
        val taskOptional = taskRepository.popById(taskDTO.id)
        assertThat(taskOptional).isNotNull
        val task = taskOptional!!
        assertThat(task.expiration).isEqualTo(taskDTO.expiration)
        assertThat(task.description.text).isEqualTo(taskDTO.description)
        assertThat(task.type).isEqualTo(taskConfig.taskType)
        assertThat(task.siteKey).isEqualTo(siteKey)
        assertThat(task.data).isInstanceOf(TextData::class.java)
    }

    @Test
    fun `evaluateAnswer success`() {
        val taskId = "taskId"
        val correctAnswer = "correctAnswer"
        val answer = TextAnswer(correctAnswer)
        val siteKey = "siteKey"
        val expiration = Instant.now().plusSeconds(10)
        val taskData = TextData(correctAnswer)
        val task = Task("Text", siteKey, expiration, Description("Type the text"), taskData)

        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        taskRepository.insertValue(taskId, task)

        val tokenDTO = verService.evaluateAnswer(taskId, answer)

        assertThat(tokenDTO.expiration.isAfter(Instant.now())).isTrue
        assertThat(taskRepository.popById(taskId)).isNull()
    }

    @Test
    fun `evaluateAnswer fail task expired`() {
        val taskId = "taskId"
        val correctAnswer = "correctAnswer"
        val answer = TextAnswer(correctAnswer)
        val siteKey = "siteKey"
        val expiration = Instant.now().minusSeconds(10)
        val taskData = TextData(correctAnswer)
        val task = Task("Text", siteKey, expiration, Description("Type the text"), taskData)

        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        taskRepository.insertValue(taskId, task)

        assertThrows<ResponseStatusException> {
            verService.evaluateAnswer(taskId, answer)
        }
    }

    @Test
    fun `evaluateAnswer fail bellow evaluation Threshold`() {
        val taskId1 = "taskId1"
        val differentTaskId = "differentTaskId"
        val correctAnswer = "correctAnswer"
        val answer = TextAnswer(correctAnswer)
        val siteKey = "siteKey"
        val expiration = Instant.now().plusSeconds(10)
        val taskData = TextData(correctAnswer)
        val task = Task("Text", siteKey, expiration, Description("Type the text"), taskData)

        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.2)
        val siteConfig = SiteConfig(siteKey, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        taskRepository.insertValue(taskId1, task)

        assertThrows<ResponseStatusException> {
            verService.evaluateAnswer(differentTaskId, answer)
        }
    }

    @Test
    fun `evaluateAnswer fail task not found`() {
        val taskId1 = "taskId1"
        val differentTaskId = "differentTaskId"
        val correctAnswer = "correctAnswer"
        val answer = TextAnswer(correctAnswer)
        val siteKey = "siteKey"
        val expiration = Instant.now().plusSeconds(10)
        val taskData = TextData(correctAnswer)
        val task = Task("Text", siteKey, expiration, Description("Type the text"), taskData)

        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySiteKey(siteKey)).thenReturn(siteConfig)

        taskRepository.insertValue(taskId1, task)

        assertThrows<ResponseStatusException> {
            verService.evaluateAnswer(differentTaskId, answer)
        }
    }

    @Test
    fun `verifyToken success`() {
        val siteKey = "siteKey"
        val tokenId = "tokenId"
        val token = Token(Instant.now().plusSeconds(10), siteKey)

        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySecretKey(secretKey)).thenReturn(siteConfig)

        tokenRepository.insertValue(tokenId, token)

        val verificationResult = verService.verifyToken(tokenId, secretKey)

        assertThat(verificationResult).isEqualTo(VerificationDTO(true))
        assertThat(tokenRepository.popById(tokenId)).isNull()
    }

    @Test
    fun `verifyToken different siteKey`() {
        val siteKey1 = "siteKey1"
        val tokenId = "tokenId"
        val token = Token(Instant.now(), siteKey1)

        val siteKey2 = "siteKey"
        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey2, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySiteKey(siteKey2)).thenReturn(siteConfig)

        tokenRepository.insertValue(tokenId, token)

        assertThrows<ResponseStatusException> {
            verService.verifyToken(tokenId, secretKey)
        }
    }

    @Test
    fun `verifyToken invalid tokenId`() {
        val siteKey1 = "siteKey1"
        val tokenIdPresent = "tokenIdPresent"
        val tokenIdChecked = "tokenIdChecked"
        val token = Token(Instant.now(), siteKey1)

        val siteKey2 = "siteKey"
        val secretKey = "secretKey"
        val taskConfig = TaskConfig("Text", EmptyGenerationConfig, 1.0)
        val siteConfig = SiteConfig(siteKey2, secretKey, "user", "configName", taskConfig)
        `when`(siteConfigRepo.getBySiteKey(siteKey2)).thenReturn(siteConfig)

        tokenRepository.insertValue(tokenIdPresent, token)

        assertThrows<ResponseStatusException> {
            verService.verifyToken(tokenIdChecked, secretKey)
        }
    }
}
