package com.example.captcha.siteconfig.dto

import com.example.captcha.siteconfig.SiteConfig

data class SiteConfigCreated(val siteKey: String, val secretKey: String) {
    companion object {
        fun fromEntity(config: SiteConfig): SiteConfigCreated {
            return SiteConfigCreated(config.siteKey, config.secretKey)
        }
    }
}
