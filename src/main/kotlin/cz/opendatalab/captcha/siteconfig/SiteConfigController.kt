package cz.opendatalab.captcha.siteconfig


import cz.opendatalab.captcha.siteconfig.dto.CreateSiteConfigDTO
import cz.opendatalab.captcha.siteconfig.dto.SiteConfigCreated
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/siteconfig")
class SiteConfigController(val siteConfigService: SiteConfigService) {

    @GetMapping
    fun getSiteConfigs(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails): List<SiteConfig> {
        return siteConfigService.getSiteConfigsForUser(user.username)
    }

    @PostMapping
    fun createSiteConfig(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody siteConfig: CreateSiteConfigDTO): SiteConfigCreated {
        val newConfig = siteConfigService.create(user.username, siteConfig.name, siteConfig.taskConfig)
        return SiteConfigCreated.fromEntity(newConfig)
    }

    @DeleteMapping("{siteKey}")
    fun deleteSiteConfig(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @PathVariable siteKey: String) {
        siteConfigService.deleteConfig(user.username, siteKey)
    }
}
