package cz.opendatalab.captcha.datamanagement

import cz.opendatalab.captcha.datamanagement.dto.*
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroup
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadata
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataService
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectCatalogue
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectService
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfo
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@RestController
@RequestMapping("api/datamanagement/objects")
class ObjectController(val metadataService: ObjectMetadataService, val objectService: ObjectService) {

    @GetMapping
    fun getDataObjects(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails): List<DataObjectDTO> {
        val metadata = metadataService.getAllAccessible(user.username).associateBy { it.objectId }
        val ids = metadata.keys
        val storageInfos = objectService.getInfoByIdList(ids)

        val result = storageInfos.map { info ->
            val storageDTO = ObjectStorageInfoDTO.from(info)
            val metadataDTO = ObjectMetadataDTO.from(metadata[info.id]!!)
            DataObjectDTO(metadataDTO, storageDTO)
        }

        return result
    }

    @GetMapping("metadata")
    fun getObjectMetadata(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails): List<ObjectMetadata> {
        return metadataService.getAllAccessible(user.username)
    }

    @PostMapping("url")
    fun addURLObject(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody urlObject: UrlObjectCreateDTO): ResponseEntity<Unit> {
        val objectId = metadataService.addUrlObject(urlObject, user.username)
        return ResponseEntity.created(URI.create("api/datamanagement/objects/$objectId")).build()
    }

    @PostMapping("file")
    fun addFileObject(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestPart("file") file: MultipartFile,
                      @RequestPart("fileObject") fileObject: FileObjectCreateDTO): ResponseEntity<Unit> {
        val objectId = metadataService.addFileObject(file, fileObject, user.username)
        return ResponseEntity.created(URI.create("api/datamanagement/objects/$objectId")).build()
    }

    @PostMapping("image/url")
    fun addURLImage(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody urlImage: UrlImageCreateDTO): ResponseEntity<List<URI>> {
        val objectIds = metadataService.addUrlImage(urlImage, user.username)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(objectIds.map{ id -> URI.create("api/datamanagement/objects/$id") })    }

    @PostMapping("image/file")
    fun addFileImage(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestPart("file") file: MultipartFile,
                      @RequestPart("fileImage") fileImage: FileImageCreateDTO): ResponseEntity<List<URI>> {
        val objectIds = metadataService.addFileImage(file, fileImage, user.username)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(objectIds.map{ id -> URI.create("api/datamanagement/objects/$id") })
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
