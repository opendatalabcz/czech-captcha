package cz.opendatalab.captcha.user

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/users")
class AdminUserController(private val userService: UserService) {

    @PostMapping
    fun createUser(@RequestBody userDetails: UserCredentials) {
        userService.createUser(userDetails.username, userDetails.password)
    }

    @GetMapping
    fun getAllUserInfo(): List<UserInfoDTO> {
        return userService.getUsers()
    }

    @PostMapping("password")
    fun updatePasswordForUser(@RequestBody updatePassword: UserCredentials) {
        userService.changePassword(updatePassword.password, updatePassword.username)
    }
}

@RestController
@RequestMapping("api/users")
class UserController(val userService: UserService) {

    @PostMapping("password")
    fun createUser(@RequestBody newPassword: String, @AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails) {
        userService.changePassword(newPassword, user.username)
    }
}

data class UserCredentials(val username: String, val password: String)
