package cz.opendatalab.captcha.verification.entities

import java.time.Instant

data class Token(val expiration: Instant, val siteKey: String)
