package com.example.captcha.initialization.mongock

import com.example.captcha.siteconfig.SiteConfig
import com.example.captcha.siteconfig.SiteConfigRepository
import com.example.captcha.siteconfig.TaskConfig
import com.example.captcha.task.templates.EmptyGenerationConfig
import com.example.captcha.task.templates.ImageLabelingGenerationConfig
import com.example.captcha.user.UserData
import com.example.captcha.user.UserRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder


@ChangeUnit(id="userdata-initializer", order = "0003", author = "ov")
class InitUserData(private val userRepo: UserRepository, private val passwordEncoder: PasswordEncoder) {
    @Execution
    fun changeSet() {
        val admin = UserData("admin",
            passwordEncoder.encode("admin"),
            mutableListOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("ROLE_ADMIN"))
        )
        val user = UserData("user",
            passwordEncoder.encode("user"),
            mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        userRepo.insert(listOf(admin, user))
    }

    @RollbackExecution
    fun rollback() {
        userRepo.deleteAll()
    }
}
