package com.example.captcha.siteconfig

import org.springframework.data.mongodb.repository.MongoRepository

interface SiteConfigRepository: MongoRepository<SiteConfig, String> {
 fun getBySiteKey(siteKey: String): SiteConfig?
 fun getBySecretKey(secretKey: String): SiteConfig?
 fun getByUserName(username: String): List<SiteConfig>
 fun deleteBySiteKey(siteKey: String)
}
