package cz.opendatalab.captcha.verification

import cz.opendatalab.captcha.Utils.generateUniqueId
import cz.opendatalab.captcha.siteconfig.SiteConfigService
import cz.opendatalab.captcha.siteconfig.TaskConfig
import cz.opendatalab.captcha.task.templates.TaskTemplateRouter
import cz.opendatalab.captcha.verification.dto.TaskDTO
import cz.opendatalab.captcha.verification.dto.TokenDTO
import cz.opendatalab.captcha.verification.dto.VerificationDTO
import cz.opendatalab.captcha.verification.entities.Answer
import cz.opendatalab.captcha.verification.entities.Task
import cz.opendatalab.captcha.verification.entities.Token
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class VerificationOrchestrationService(private val siteConfigService: SiteConfigService,
                                       private val config: VerificationConfig,
                                       private val taskTemplateRouter: TaskTemplateRouter,
                                       private val taskRepository: KeyValueStore<Task>,
                                       private val tokenRepository: KeyValueStore<Token>
) {
    fun generateTask(siteKey: String): TaskDTO {
        // 1. Based on siteKey generate task;
        // or throw exception when siteKey does not exist
        val (verificationConfig, username) = siteConfigService.getTaskConfig(siteKey)

        return createTask(verificationConfig, username, siteKey)
    }

    fun evaluateAnswer(taskId: String, answer: Answer): TokenDTO {
        val task = taskRepository.popById(taskId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found by taskId $taskId, Invalid taskId or task expired")

        checkExpiration(task.expiration, "Task expired")

        val evaluation = taskTemplateRouter.evaluateTask(task.type, task.data, answer)

        val (taskConfig, _) = siteConfigService.getTaskConfig(task.siteKey)

        if (evaluation.evaluation < taskConfig.evaluationThreshold) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad answer")
        }

        val (token, tokenId) = createToken(task.siteKey)

        return TokenDTO.fromToken(tokenId, token)
    }

    fun verifyToken(tokenId: String, secretKey: String): VerificationDTO {
        val token = tokenRepository.popById(tokenId) ?:
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Token not found by tokenId $tokenId, Invalid tokenId or task expired")

        checkExpiration(token.expiration, "Token expired")

        checkSecretKey(secretKey, token.siteKey)

        return VerificationDTO(true)
    }

    private fun createToken(siteKey: String): Pair<Token, String> {
        val token = Token(createExpiration(config.token.expiration), siteKey)
        val tokenId = generateUniqueId()
        tokenRepository.insertValue(tokenId, token)
        return Pair(token, tokenId)
    }

    private fun createTask(verConfig: TaskConfig, userName: String, siteKey: String): TaskDTO {
        val (description, data, answerSheet) = taskTemplateRouter.generateTask(verConfig.taskType, userName, verConfig.generationConfig)

        val expiration = createExpiration(config.task.expiration)
        val task = Task(verConfig.taskType, siteKey, expiration, description, data)
        val taskId = generateUniqueId()
        taskRepository.insertValue(taskId, task)
        return TaskDTO.fromTask(task, taskId, answerSheet)
    }

    private fun createExpiration(minutesToComplete: Long): Instant {
        return Instant.now().plus(minutesToComplete, ChronoUnit.MINUTES)
    }

    private fun checkExpiration(expiration: Instant, exceptionMessage: String) {
        if (expiration.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, exceptionMessage)
        }
    }

    private fun checkSecretKey(secretKey: String, tokenSiteKey: String) {
        val siteKey = siteConfigService.secretKeyToSiteKey(secretKey) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid secretKey - secretKey not found")

        if (siteKey != tokenSiteKey) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Site key for given token does not match your site")
        }
    }
}
