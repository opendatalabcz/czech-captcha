package cz.opendatalab.captcha.verification

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "verification")
data class VerificationProperties @ConstructorBinding constructor(val token: TokenProperties, val task: TaskProperties) {
    data class TokenProperties(val expiration: Long)
    data class TaskProperties(val expiration: Long)
}
