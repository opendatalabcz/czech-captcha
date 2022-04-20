package com.example.captcha.user

import org.springframework.http.HttpStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(val inMemoryUserDetailsManager: InMemoryUserDetailsManager,
                  val passwordEncoder: PasswordEncoder) {
    fun userExists(userName: String): Boolean {
        return inMemoryUserDetailsManager.userExists(userName)
    }

    fun createUser(userName: String, password: String) {
        if (userExists(userName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists")
        }

        val hashedPassword = passwordEncoder.encode(password)
        val user = org.springframework.security.core.userdetails.User(userName, hashedPassword, listOf(SimpleGrantedAuthority("ROLE_USER")))
        inMemoryUserDetailsManager.createUser(user)
    }
}
