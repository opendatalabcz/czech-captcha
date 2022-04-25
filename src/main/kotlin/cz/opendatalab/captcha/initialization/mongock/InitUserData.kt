package cz.opendatalab.captcha.initialization.mongock

import cz.opendatalab.captcha.user.UserData
import cz.opendatalab.captcha.user.UserRepository
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import io.mongock.api.annotations.RollbackExecution
import org.springframework.security.crypto.password.PasswordEncoder


@ChangeUnit(id="userdata-initializer", order = "0003", author = "ov")
class InitUserData(private val userRepo: UserRepository, private val passwordEncoder: PasswordEncoder) {
    @Execution
    fun changeSet() {
        val admin = UserData("admin",
            passwordEncoder.encode("admin"),
            listOf("ROLE_USER", "ROLE_ADMIN")
        )
        val user = UserData("user",
            passwordEncoder.encode("user"),
            listOf("ROLE_USER")
        )

        userRepo.insert(listOf(admin, user))
    }

    @RollbackExecution
    fun rollback() {
        userRepo.deleteAll()
    }
}
