package cz.opendatalab.captcha.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles


@SpringBootTest
@ActiveProfiles("test")
internal class UserServiceTest(
    @Autowired private val userService: UserService
) {
    @MockBean
    private lateinit var userRepository: UserRepository

    @Test
    fun `userExists exists`() {
        val username = "username"
        Mockito.`when`(userRepository.existsByUsername(username)).thenReturn(true)

        assertThat(userService.userExists(username)).isTrue
    }

    @Test
    fun `userExists does not exist` () {
        val username = "username"
        Mockito.`when`(userRepository.existsByUsername(username)).thenReturn(false)

        assertThat(userService.userExists(username)).isFalse
    }

    @Test
    fun `createUser test`() {
        val username = "username"
        val password = "password"

        userService.createUser(username, password)

        Mockito.verify(userRepository).insert(isA(UserData::class.java))
    }

    @Test
    fun loadUserByUsername() {
        val username = "username"
        val user = UserData(username, "password", emptySet())

        Mockito.`when`(userRepository.findByUsername(username)).thenReturn(user)

        assertThat(userService.loadUserByUsername(username)).isEqualTo(user)
    }

    @Test
    fun changePassword() {
        val username = "username"
        val newPassword = "password"

        val user = UserData(username, "hashedOldPassword", emptySet())
        Mockito.`when`(userRepository.findByUsername(username)).thenReturn(user)

        userService.changePassword(newPassword, username)

        Mockito.verify(userRepository).findByUsername(username)
        Mockito.verify(userRepository).save(isA(UserData::class.java))
    }
}
