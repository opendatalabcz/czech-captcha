package cz.opendatalab.captcha.verification

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConfigurationProperties(prefix = "verification")
data class VerificationConfig @ConstructorBinding constructor(val token: TokenConfiguration, val task: TaskConfiguration) {
    data class TokenConfiguration(val expiration: Long)
    data class TaskConfiguration(val expiration: Long)
}
