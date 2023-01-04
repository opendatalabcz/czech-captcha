package cz.opendatalab.captcha.datamanagement

import cz.opendatalab.captcha.datamanagement.dto.*
import cz.opendatalab.captcha.datamanagement.objectmetadata.LabelGroup
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadata
import cz.opendatalab.captcha.datamanagement.objectmetadata.ObjectMetadataService
import cz.opendatalab.captcha.datamanagement.objectstorage.ObjectStorageInfoRepository
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
class ObjectController(
    private val metadataService: ObjectMetadataService,
    private val objectService: ObjectService
    ) {

    @GetMapping
    fun getDataObjects(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails): List<DataObjectDTO> {
        val metadata = metadataService.getAllAccessible(user.username).associateBy { it.id }
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
    fun getAllObjectMetadata(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails): List<ObjectMetadata> {
        return metadataService.getAllAccessible(user.username)
    }

    @PostMapping("url")
    fun addURLObject(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody urlObject: UrlObjectCreateDTO): ResponseEntity<Unit> {
        val metadata = metadataService.addUrlObject(urlObject, user.username)
        return ResponseEntity.created(URI.create("api/datamanagement/objects/${metadata.id}")).build()
    }

    @PostMapping("file")
    fun addFileObject(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestPart("file") file: MultipartFile,
                      @RequestPart("fileObject") fileObject: FileObjectCreateDTO): ResponseEntity<Unit> {
        val originalFilename = file.originalFilename ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Original filename of uploaded file cannot be null")
        val metadata = metadataService.addFileObject(file.inputStream, originalFilename, fileObject, user.username)
        return ResponseEntity.created(URI.create("api/datamanagement/objects/${metadata.id}")).build()
    }

    @PostMapping("image/url")
    fun addURLImage(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestBody urlImage: UrlImageCreateDTO): ResponseEntity<List<URI>> {
        val objectsMetadata = metadataService.addUrlImageWithOD(urlImage, user.username)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(objectsMetadata.map{ objectMetadata -> URI.create("api/datamanagement/objects/${objectMetadata.id}") })    }

    @PostMapping("image/file")
    fun addFileImage(@AuthenticationPrincipal @Parameter(hidden = true) user: UserDetails, @RequestPart("file") file: MultipartFile,
                      @RequestPart("fileImage") fileImage: FileImageCreateDTO): ResponseEntity<List<URI>> {
        val originalFilename = file.originalFilename ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Original filename of uploaded file cannot be null")
        val objectsMetadata = metadataService.addFileImageWithOD(file.inputStream, originalFilename, fileImage, user.username)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(objectsMetadata.map{ objectMetadata -> URI.create("api/datamanagement/objects/${objectMetadata.id}") })
    }

    @GetMapping("labelgroups")
    fun getLabelGroups(): List<LabelGroup> {
        return metadataService.getLabelGroups()
    }

    @GetMapping("labelgroups/{name}")
    fun getLabelGroup(@PathVariable name: String): LabelGroup {
        return metadataService.getLabelGroup(name)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Labelgroup $name not found.")
    }

    @PostMapping("labelgroups")
    fun createLabelGroup(@RequestBody labelGroup: LabelGroupCreateDTO): ResponseEntity<Unit> {
        val created = metadataService.createLabelGroup(labelGroup)
        return ResponseEntity.created(URI.create("api/datamanagement/objects/labelGroup/${created.name}")).build()
    }
}

@RestController
@RequestMapping("api/admin/datamanagement/objects")
class ObjectAdminController(val metadataService: ObjectMetadataService,
                            val objectStorageInfoRepository: ObjectStorageInfoRepository) {
    @GetMapping("metadata")
    fun getObjectMetadata(): List<ObjectMetadata> {
        return metadataService.getAll()
    }

    @GetMapping("storageinfo/{objectId}")
    fun getObjectInfo(@PathVariable objectId: String): ObjectStorageInfo {
        return objectStorageInfoRepository.findByIdOrNull(objectId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Object with id $objectId not found")
    }

    @GetMapping("storageinfo")
    fun getObjectInfoList(): List<ObjectStorageInfo> {
        return objectStorageInfoRepository.findAll()
    }

    @GetMapping("{objectId}")
    fun getObject(@PathVariable objectId: String): ObjectDTO {
        val objectInfo = objectStorageInfoRepository.findByIdOrNull(objectId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Object with id $objectId not found in catalogue")
        val metadata = metadataService.getById(objectId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Metadata for object with id $objectId not found")
        return ObjectDTO(objectInfo, metadata)
    }
}
