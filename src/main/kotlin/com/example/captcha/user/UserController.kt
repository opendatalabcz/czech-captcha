package com.example.captcha.user

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/admin/users")
class UserController(val userService: UserService) {

    @PostMapping
    fun createUser(@RequestBody userDetails: UserCredentials) {
        userService.createUser(userDetails.username, userDetails.password)
    }

}


data class UserCredentials(val username: String, val password: String)
