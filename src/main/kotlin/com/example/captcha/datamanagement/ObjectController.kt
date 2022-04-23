package com.example.captcha.datamanagement

import com.example.captcha.datamanagement.dto.LabelGroupCreateDTO
import com.example.captcha.datamanagement.objectmetadata.ObjectMetadata
import com.example.captcha.datamanagement.objectmetadata.ObjectMetadataService
import com.example.captcha.datamanagement.objectmetadata.LabelGroup
import com.example.captcha.datamanagement.dto.ObjectDTO
import com.example.captcha.datamanagement.dto.UrlObjectCreateDTO
import com.example.captcha.datamanagement.objectstorage.ObjectCatalogue
import com.example.captcha.datamanagement.objectstorage.ObjectStorageInfo
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@RestController
@RequestMapping("api/datamanagement/objects")
class ObjectController(val metadataService: ObjectMetadataService) {

    @GetMapping("metadata")
    fun getObjectMetadata(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails): List<ObjectMetadata> {
        return metadataService.getAllAccessible(user.username)
    }

    @PostMapping("url")
    fun addURLObject(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody urlObject: UrlObjectCreateDTO): ResponseEntity<Unit> {
        val objectId = metadataService.addUrlObject(urlObject, user.username)
        return ResponseEntity.created(URI.create("api/datamanagement/objects/$objectId")).build()
    }

    @GetMapping("labelgroups")
    fun getLabelGroup(): List<LabelGroup> {
        return metadataService.getLabelGroups()
    }

    @PostMapping("labelgroups")
    fun createLabelGroup(@RequestBody labelGroup: LabelGroupCreateDTO) {
        return metadataService.createLabelGroup(labelGroup)
    }
}

@RestController
@RequestMapping("api/admin/datamanagement/objects")
class ObjectAdminController(val metadataService: ObjectMetadataService,
                            val objectCatalogue: ObjectCatalogue) {
    @GetMapping("metadata")
    fun getObjectMetadata(): List<ObjectMetadata> {
        return metadataService.getAll()
    }

    @GetMapping("storageinfo/{objectId}")
    fun getObjectInfo(@PathVariable objectId: String): ObjectStorageInfo {
        return objectCatalogue.findByIdOrNull(objectId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Object with id $objectId not found")
    }

    @GetMapping("storageinfo")
    fun getObjectInfoList(): List<ObjectStorageInfo> {
        return objectCatalogue.findAll()
    }

    @GetMapping("{objectId}")
    fun getObject(@PathVariable objectId: String): ObjectDTO {
        val objectInfo = objectCatalogue.findByIdOrNull(objectId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Object with id $objectId not found in catalogue")
        val metadata = metadataService.getById(objectId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Metadata for object with id $objectId not found")
        return ObjectDTO(objectInfo, metadata)
    }
}
