package cz.opendatalab.captcha.siteconfig


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
    fun createSiteConfig(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody taskConfig: TaskConfigDTO): SiteConfigCreated {
        val newConfig = siteConfigService.create(user.username, taskConfig)
        return SiteConfigCreated.fromEntity(newConfig)
    }

    @DeleteMapping
    fun deleteSiteConfig(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody siteKey: String) {
        siteConfigService.deleteConfig(user.username, siteKey)
    }
}
