package cz.opendatalab.captcha.siteconfig.dto

import cz.opendatalab.captcha.siteconfig.SiteConfig

data class SiteConfigCreated(val siteKey: String, val secretKey: String) {
    companion object {
        fun fromEntity(config: SiteConfig): SiteConfigCreated {
            return SiteConfigCreated(config.siteKey, config.secretKey)
        }
    }
}
