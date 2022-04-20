package com.example.captcha.verification.dto

import com.example.captcha.verification.entities.Token
import java.time.Instant

data class TokenDTO(val id: String, val expiration: Instant, val siteKey: String) {
    companion object {
        fun fromToken(id: String, token: Token): TokenDTO {
            return TokenDTO(id, token.expiration, token.siteKey)
        }
    }
}
