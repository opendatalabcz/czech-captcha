package com.example.captcha.siteconfig


interface SiteConfigRepository {
    fun getAll(): List<SiteConfig>
    fun getBySiteKey(siteKey: String): SiteConfig?
    fun getBySecretKey(secretKey: String): SiteConfig?
    fun getByUsername(username: String): List<SiteConfig>
    fun add(siteConfig: SiteConfig): SiteConfig
}
