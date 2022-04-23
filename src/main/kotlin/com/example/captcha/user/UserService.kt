package com.example.captcha.user

import org.springframework.http.HttpStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(private val userRepo: UserRepository, val passwordEncoder: PasswordEncoder): UserDetailsService {
    fun userExists(userName: String): Boolean {
        return userRepo.existsByUsername(userName)
    }

    fun createUser(userName: String, password: String) {
        if (userExists(userName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists")
        }

        val hashedPassword = passwordEncoder.encode(password)
        val user = UserData(userName, hashedPassword, mutableListOf(SimpleGrantedAuthority("ROLE_USER")))
        userRepo.insert(user)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        return userRepo.findByUsername(username)
            ?: throw UsernameNotFoundException("User with username $username not found")
    }
}
