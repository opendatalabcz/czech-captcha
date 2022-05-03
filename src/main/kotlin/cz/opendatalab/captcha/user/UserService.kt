package cz.opendatalab.captcha.user

import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(private val userRepo: UserRepository, private val passwordEncoder: PasswordEncoder): UserDetailsService {
    fun userExists(userName: String): Boolean {
        return userRepo.existsByUsername(userName)
    }

    fun getUsers(): List<UserInfoDTO> {
        return userRepo.findAll()
            .map { userData -> UserInfoDTO(userData.username, userData.authorities) }
    }

    fun createUser(userName: String, password: String) {
        if (userExists(userName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists")
        }

        val hashedPassword = passwordEncoder.encode(password)
        val user = UserData(userName, hashedPassword, listOf("ROLE_USER"))
        userRepo.insert(user)
    }

    override fun loadUserByUsername(username: String): UserDetails {
        return userRepo.findByUsername(username)
            ?: throw UsernameNotFoundException("User with username $username not found")
    }

    fun changePassword(password: String, username: String) {
        val user = userRepo.findByUsername(username)
            ?: throw UsernameNotFoundException("User with username $username not found")

        user.password = passwordEncoder.encode(password)

        userRepo.save(user)
    }
}

data class UserInfoDTO(val userName: String, val authorities: List<String>)
