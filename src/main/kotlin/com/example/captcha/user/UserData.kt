package com.example.captcha.user

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Document("user")
data class UserData(@Id private val username: String, private var password: String, val authorities: List<String>): UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return authorities.map { SimpleGrantedAuthority(it) }.toMutableList()
    }

    override fun getPassword(): String {
        return password
    }

    fun setPassword(newPassword: String) {
        password = newPassword
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}
