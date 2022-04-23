package com.example.captcha.user

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/users")
class UserController(val userService: UserService) {

    @PostMapping
    fun createUser(@RequestBody userDetails: UserCredentials) {
        userService.createUser(userDetails.username, userDetails.password)
    }

    @GetMapping
    fun getAllUserInfo(): List<UserInfoDTO> {
        return userService.getUsers()
    }
}


data class UserCredentials(val username: String, val password: String)
