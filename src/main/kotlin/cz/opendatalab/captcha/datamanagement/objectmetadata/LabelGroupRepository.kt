package cz.opendatalab.captcha.datamanagement.objectmetadata

import org.springframework.data.mongodb.repository.MongoRepository

interface LabelGroupRepository: MongoRepository<LabelGroup, String> {
    fun findByName(labelGroupName: String): LabelGroup?
    fun existsByName(name: String): Boolean
    fun deleteByName(name: String)
}
