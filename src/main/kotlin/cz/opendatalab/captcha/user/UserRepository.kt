package cz.opendatalab.captcha.user

import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository: MongoRepository<UserData, String> {
    fun existsByUsername(username: String): Boolean
    fun findByUsername(username: String): UserData?
}
