import {
    getObjects,
    uploadUrlImage,
    uploadUrlObject,
    uploadFileImage,
    uploadFileObject
} from "./api.js";

const FILE_EXTENSIONS = Object.freeze({
    image: ["jpg", "jpeg", "png", "gif", "svg", "apng", "bmp", "pjpeg", "svg+xml", "tiff", "webp", "x-icon"],
    sound: ["mp3", "aac", "wav", "mp4", "wma", "flac", "m4a"],
    getObjectType: function(extension) {
        if (this.image.includes(extension)) {
            return OBJECTS_TYPES.Image
        }
        if (this.sound.includes(extension)) {
            return OBJECTS_TYPES.Sound
        }
        return OBJECTS_TYPES.Text
    }
});
const OBJECTS_TYPES = Object.freeze({
    Image: "Image",
    Sound: "Sound",
    Text: "Text"
});
const SOURCE_TYPES = Object.freeze({
    URL: "URL",
    File: "File",
    Directory: "Directory"
});
const getUrlWithoutParameters = function (url) {
    const urlObj = new URL(url);
    urlObj.search = '';
    return urlObj.toString();
}
const getExtensionFromFilename = function (filename) {
    if (! !!filename) {
        return "";
    }
    return filename.split(".").pop().toLowerCase();
}
const getObjectTypeFromExtension = function(extension) {
    if (FILE_EXTENSIONS.image.includes(extension)) {
        return OBJECTS_TYPES.Image
    }
    if (FILE_EXTENSIONS.sound.includes(extension)) {
        return OBJECTS_TYPES.Sound
    }
    return OBJECTS_TYPES.Text
}
const getObjectTypeFromFilename = function (filename) {
    return getObjectTypeFromExtension(getExtensionFromFilename(filename))
}

class ObjectMetadataCreateDTO {
    constructor(knownLabels, tags) {
        this.knownLabels = knownLabels;
        this.tags = tags;
    }
}

class ObjectDetectionDTO {
    constructor(objectDetectionParameters, annotations) {
        this.objectDetectionParameters = objectDetectionParameters;
        this.annotations = annotations;
    }
}

class ObjectDetectionParametersDTO {
    constructor(wantedLabels, thresholdOneVote, thresholdTwoVotes) {
        this.wantedLabels = wantedLabels;
        this.thresholdOneVote = thresholdOneVote;
        this.thresholdTwoVotes = thresholdTwoVotes;
    }
}

class AnnotationDTO {
    constructor(labelGroup, label, boundingBox) {
        this.labelGroup = labelGroup;
        this.label = label;
        this.boundingBox = boundingBox;
    }
}

class ObjectToUpload {
    constructor(id, sourceType, filename, dto) {
        this.id = id;
        this.sourceType = sourceType;
        this.objectType = getObjectTypeFromFilename(filename);
        this.dto = dto;
        this.isUploading = false;
    }
    upload() {}
    isImage() {
        return this.objectType === OBJECTS_TYPES.Image;
    }
    addMetadata(metadata) {
        this.dto.metadata = metadata;
    }
    addODData(odParams, annotations) {
        if (this.isImage()) {
            this.dto.objectDetection = new ObjectDetectionDTO(odParams, annotations)
        }
    }
}

class UrlObjectToUpload extends ObjectToUpload {
    constructor(url) {
        super(url, SOURCE_TYPES.URL, getUrlWithoutParameters(url), { url: url });
    }
    upload() {
        this.isUploading = true;
        const uploadFunction = this.getUploadFunction();
        return uploadFunction(this.dto);
    }
    getUploadFunction() {
        return this.objectType === OBJECTS_TYPES.Image ? uploadUrlImage : uploadUrlObject;
    }
}

class FileObjectToUpload extends ObjectToUpload {
    constructor(file) {
        super(file.name, SOURCE_TYPES.File, file.name, {});
        this.file = file;
    }

    upload() {
        this.isUploading = true;
        const uploadFunction = this.getUploadFunction();
        return uploadFunction(this.file, this.dto);
    }

    getUploadFunction() {
        return this.objectType === OBJECTS_TYPES.Image ? uploadFileImage : uploadFileObject;
    }
}

class ObjectsToUpload {
    constructor() {
        this.toUpload = {};
        this.imagesCount = 0;
    }
    getCount() {
        return Object.keys(this.toUpload).length;
    }
    addUrlObject(url) {
        const toAdd = new UrlObjectToUpload(url);
        this.toUpload[url] = toAdd;
        if (toAdd.isImage()) {
            this.imagesCount++;
        }
    }
    addFileObject(file) {
        const toAdd = new FileObjectToUpload(file);
        this.toUpload[file.name] = toAdd;
        if (toAdd.isImage()) {
            this.imagesCount++;
        }
    }
    remove(objectId) {
        const toRemove = this.toUpload[objectId];
        delete this.toUpload[objectId];
        if (toRemove.isImage()) {
            this.imagesCount--;
        }
    }
    addMetadataToAllObjects(metadata) {
        Object.values(this.toUpload).forEach(objToUpload => objToUpload.addMetadata(metadata));
    }
    addODDataToAllObjects(odParams, allAnnotations) {
        Object.values(this.toUpload).forEach(objToUpload => objToUpload.addODData(odParams, null));
    }
    uploadAllObjects() {
        return Object.values(this.toUpload).map(objToUpload => {
            const res = objToUpload.upload();
            res.catch(error => {
                    console.log(error);
                    confirm("Object " + objToUpload.id + " was not uploaded: " + error);
                })
                .finally(_ => this.remove(objToUpload.id));
            return res;
        });
    }
}

const SiteConfig = {
    data() {
        return {
            objectTypes: OBJECTS_TYPES,
            sources: SOURCE_TYPES,

            tags: [],
            objectDetection: false,
            odWantedLabels: {},

            source: "",
            enteredUrl: "",
            enteredTag: "",
            enteredLabelGroup: "",
            enteredLabel: "",
            odThreshold1: 0,
            odThreshold2: 0,

            objectsToUpload: new ObjectsToUpload(),
            existingObjects: [],
            opened: new Set()
        }
    },
    methods: {
        // objects view table ----------------------------------------------------------------------------
        updateObjects() {
            getObjects().then(response => {
                this.existingObjects = response.data;
                this.opened = new Set();
            });
        },
        toggleRow(index) {
            if (this.isOpened(index)) {
                this.opened.delete(index);
            } else {
                this.opened.add(index);
            }
        },
        isOpened(index) {
            return this.opened.has(index);
        },
        // form filling ----------------------------------------------------------------------------
        addTag() {
            if (!!this.enteredTag && !this.tags.includes(this.enteredTag)) {
                this.tags.push(this.enteredTag);
                this.enteredTag = "";
            }
        },
        removeTag(index) {
            if (this.tags.length > index) {
                this.tags.splice(index, 1);
            }
        },
        addLabel() {
            if (!!this.enteredLabelGroup && !!this.enteredLabel) {
                if (this.odWantedLabels.hasOwnProperty(this.enteredLabelGroup)) {
                    if (!this.odWantedLabels[this.enteredLabelGroup].includes(this.enteredLabel)) {
                        this.odWantedLabels[this.enteredLabelGroup].push(this.enteredLabel)
                    }
                } else {
                    this.odWantedLabels[this.enteredLabelGroup] = [this.enteredLabel]
                }
                this.enteredLabel = "";
            }
        },
        removeLabel(labelGroup, label) {
            if (!this.odWantedLabels.hasOwnProperty(labelGroup)) {
                return;
            }
            const index = this.odWantedLabels[labelGroup].indexOf(label);
            if (index <= -1) {
                return;
            }
            this.odWantedLabels[labelGroup].splice(index, 1);
            if (this.odWantedLabels[labelGroup].length === 0) {
                delete this.odWantedLabels[labelGroup];
            }
        },
        emptyForm() {
            this.objectType = "";
            this.objectSource = "";
            this.enteredUrl = "";
            this.objectFile = null;
            this.objectFormat = "";
            this.tags = [];
            this.enteredTag = "";
            this.objectDetection = false;
            this.odWantedLabels = {};
            this.odThreshold1 = 0;
            this.odThreshold2 = 0;
        },
        handleChangeUrl() {
            this.objectsToUpload.addUrlObject(this.enteredUrl);
        },
        handleFileUpload() {
            this.$refs.objectFile.files.forEach(file => this.objectsToUpload.addFileObject(file));
            this.$refs.objectFile.value = null;
        },
        handleDirUpload() {
            this.$refs.objectDir.files.forEach(file => this.objectsToUpload.addFileObject(file));
            this.$refs.objectDir.value = null;
        },
        // files upload ------------------------------------------------------------------------------------
        createObject() {
            if (!this.validateForm()) {
                return;
            }
            this.objectsToUpload.addMetadataToAllObjects(new ObjectMetadataCreateDTO({}, this.tags));
            const odParams = new ObjectDetectionParametersDTO(this.odWantedLabels, this.odThreshold1, this.odThreshold2);
            this.objectsToUpload.addODDataToAllObjects(odParams, null);
            const res = this.objectsToUpload.uploadAllObjects();
            res.forEach(objResult => objResult.then(_ => this.updateObjects()))
            this.emptyForm();
        },
        validateForm() {
            if (this.objectsToUpload.getCount() <= 0) {
                confirm("Select at least one object to upload.");
                return false;
            }
            // if (this.odThreshold1 < 0.0 || this.odThreshold1 > 1.0 ||
            //     this.odThreshold2 < 0.0 || this.odThreshold2 > 1.0) {
            //     confirm("Thresholds for object detection must be between 0 and 1.");
            //     return false;
            // }
            // if (this.odThreshold1 > this.odThreshold2) {
            //     confirm("Threshold 1 must be lower than or equal to Threshold 2.");
            //     return false;
            // }
            return true;

        }
    },
    mounted() {
        this.updateObjects();
    }
}

Vue.createApp(SiteConfig).mount('#app')
