package cz.opendatalab.captcha.siteconfig.dto

import cz.opendatalab.captcha.siteconfig.TaskConfigDTO

data class CreateSiteConfigDTO(val name: String, val taskConfigDTO: TaskConfigDTO)
