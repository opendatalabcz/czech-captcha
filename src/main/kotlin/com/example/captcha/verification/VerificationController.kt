package com.example.captcha.verification

import com.example.captcha.verification.dto.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/verification")
class VerificationController(private val verificationOrchestrationService: VerificationOrchestrationService) {

    @PostMapping("tasks")
    fun createTask(@RequestParam("siteKey") siteKey: String): TaskDTO {
        return verificationOrchestrationService.generateTask(siteKey)
    }

    /**
     * Evaluate task and create token
     */
    @PostMapping("tokens")
    fun createToken(@RequestBody answer: AnswerDTO): TokenDTO {
        return verificationOrchestrationService.evaluateAnswer(answer.taskId, answer.data)
    }

    /**
     * Verify token
     */
    @PostMapping("tokens/verification")
    fun verifyToken(@RequestBody tokenVerification: TokenVerificationDTO): VerificationDTO {
        return verificationOrchestrationService.verifyToken(tokenVerification.tokenId, tokenVerification.secretKey)
    }
}
