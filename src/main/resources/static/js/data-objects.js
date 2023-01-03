import {
    getObjects,
    uploadUrlImage,
    uploadUrlObject,
    uploadFileImage,
    uploadFileObject
} from "./api.js";
import {formatToArrayString, isEmptyObject, isTableRowOpened, toggleTableRow} from "./common.js";
import {COCO_SCHEMA} from "./coco-schema.js";

const validateUsingCocoSchema = new Ajv().compile(COCO_SCHEMA);

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

const createCategoryMapping = function (categories) {
    const mapping = {};
    for (let i = 0; i < categories.length; i++) {
        mapping[categories[i].id] = {
            label: categories[i].name,
            labelGroup: categories[i].supercategory ? categories[i].supercategory : "all"
        };
    }
    return mapping;
}
const createFilenameMapping = function (images) {
    const mapping = {};
    for (let i = 0; i < images.length; i++) {
        mapping[images[i].file_name] = images[i];
    }
    return mapping;
}
const createAnnotationsMapping = function (annotations) {
    const mapping = {};
    for (let i = 0; i < annotations.length; i++) {
        if (!mapping.hasOwnProperty(annotations[i].image_id)) {
            mapping[annotations[i].image_id] = [];
        }
        mapping[annotations[i].image_id].push(annotations[i]);
    }
    return mapping;
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

class ObjectToUpload {
    constructor(id, sourceType, filename, dto) {
        this.id = id;
        this.sourceType = sourceType;
        this.objectType = getObjectTypeFromFilename(filename);
        this.dto = dto;
        this.isUploading = false;
        if (this.isImage()) {
            this.addODField(null, null)
        }
    }
    upload() {}
    isImage() {
        return this.objectType === OBJECTS_TYPES.Image;
    }
    containsAnnotations() {
        return !!(this.dto?.objectDetection?.annotations);
    }
    addMetadata(metadata) {
        this.dto.metadata = metadata;
    }
    addODParams(odParams) {
        if (!this.isImage()) {
            return;
        }
        this.addODField(odParams, null);
    }
    addAnnotations(filenameMapping, annotationsMapping, categoryMapping) {
        if (!this.isImage()) {
            return;
        }
        const annotations = this.extractAnnotations(filenameMapping, annotationsMapping, categoryMapping);
        this.addODField(null, annotations);
    }
    removeAnnotations() {
        if (!this.dto.objectDetection) {
            return;
        }
        this.dto.objectDetection.annotations = null;
    }
    addODField(odParams, annotations) {
        if (!this.dto.objectDetection) {
            this.dto.objectDetection = new ObjectDetectionDTO(odParams, annotations);
            return;
        }
        if (odParams) {
            this.dto.objectDetection.objectDetectionParameters = odParams;
        }
        if (annotations) {
            this.dto.objectDetection.annotations = annotations;
        }
    }
    extractAnnotations(filenameMapping, annotationsMapping, categoryMapping) {
        const image = filenameMapping[this.id];
        if (!image) {
            return null;
        }
        const annotations = annotationsMapping[image.id];
        if (!annotations) {
            return null;
        }
        const annotationDtos = this.mapAnnotations(annotations, categoryMapping, image.width, image.height)
        return annotationDtos.length === 0 ? null : annotationDtos;
    }
    calculateBoundingBox(bbox, width, height) {
        return {
            x:      bbox[0] / width,
            y:      bbox[1] / height,
            width:  bbox[2] / width,
            height: bbox[3] / height
        };
    }
    mapAnnotations(annotations, categoryMapping, width, height) {
        return annotations.map( annotation => {
            return {
                ...(categoryMapping[annotation.category_id]),
                boundingBox: this.calculateBoundingBox(annotation.bbox, width, height)
            }
        });
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
    addODParamsToAllObjects(odParams) {
        Object.values(this.toUpload).forEach(objToUpload => objToUpload.addODParams(odParams));
    }
    addAnnotationsToAllObjects(filenameMapping, annotationsMapping, categoryMapping) {
        Object.values(this.toUpload).forEach(objToUpload => objToUpload.addAnnotations(filenameMapping, annotationsMapping, categoryMapping));
    }
    removeAnnotationsFromAllObjects() {
        Object.values(this.toUpload).forEach(objToUpload => objToUpload.removeAnnotations());
    }
    uploadAllObjects() {
        return Object.values(this.toUpload).map(objToUpload => {
            const res = objToUpload.upload();
            res.catch(error => {
                    console.log(error);
                    alert("Object " + objToUpload.id + " was not uploaded: " + error);
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

            showNewDropDown: false,
            showNewModal: false,
            openedRows: new Set(),

            tags: [],
            doObjectDetection: false,
            addAnnotations: false,
            odWantedLabels: {},

            source: "",
            enteredUrl: "",
            enteredTag: "",
            enteredLabelGroup: "",
            enteredLabel: "",
            odThreshold1: 0,
            odThreshold2: 0,

            objectsToUpload: new ObjectsToUpload(),
            existingObjects: []
        }
    },
    methods: {
        toggleRow(index) { toggleTableRow(this.openedRows, index); },
        isRowOpened(index) { return isTableRowOpened(this.openedRows, index); },
        isEmptyObject(obj) { return isEmptyObject(obj); },
        getArrayString(array) { return formatToArrayString(array); },
        getLabelGroupString(labelGroup) {
            if (labelGroup.isLabeled) {
                const positive = labelGroup.labels.join("(3), ") + "(3)";
                const negative = labelGroup.negativeLabels.join("(-3), ") + "(-3)";
                return positive + ", " + negative;
            } else {
                const labels = [];
                for (let label in labelGroup.labelStatistics.statistics) {
                    labels.push(label + "(" + labelGroup.labelStatistics.statistics[label].value + ")");
                }
                return labels.join(", ");
            }
        },
        getAddDataObjectButtonText() {
            const count = this.objectsToUpload.getCount();
            if (count === 1) {
                return "Add " + 1 + " Data Object"
            }
            return "Add " + count + " Data Objects"
        },
        // objects view table ----------------------------------------------------------------------------
        updateObjects() {
            getObjects().then(response => {
                this.existingObjects = response.data;
                this.openedRows = new Set();
            });
        },
        // form filling ----------------------------------------------------------------------------
        selectDataSource(sourceType) {
            this.source = sourceType;
            this.showNewDropDown = false;
            this.showNewModal = true;
        },
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
            this.showNewModal = false;
            this.objectType = "";
            this.objectSource = "";
            this.enteredUrl = "";
            this.objectFile = null;
            this.objectFormat = "";
            this.tags = [];
            this.enteredTag = "";
            this.doObjectDetection = false;
            this.addAnnotations = false;
            this.odWantedLabels = {};
            this.odThreshold1 = 0;
            this.odThreshold2 = 0;
        },
        handleUrlAdd() {
            try {
                this.objectsToUpload.addUrlObject(this.enteredUrl);
            } catch (e) {
                alert("Error with the URL: " + e.message);
            }
        },
        handleFileUpload() {
            this.selectDataSource(this.sources.File);
            this.$refs.objectFile.files.forEach(file => this.objectsToUpload.addFileObject(file));
            this.$refs.objectFile.value = null;
        },
        handleDirUpload() {
            this.selectDataSource(this.sources.Directory);
            this.$refs.objectDir.files.forEach(file => this.objectsToUpload.addFileObject(file));
            this.$refs.objectDir.value = null;
        },
        handleAnnotationsUpload() {
            this.objectsToUpload.removeAnnotationsFromAllObjects();
            const file = this.$refs.annotationsFile.files[0];
            if (!file) {
                return;
            }
            const reader = new FileReader();
            reader.onload = (e) => {
                const jsonData = e.target.result;
                let cocoObject;
                try {
                    cocoObject = JSON.parse(jsonData);
                } catch (e) {
                    alert("File with annotations could not be parsed as JSON.");
                    this.$refs.annotationsFile.value = null;
                    return;
                }
                if (!validateUsingCocoSchema(cocoObject)) {
                    alert("File is not in valid COCO format: " + validateUsingCocoSchema.errors.join(" "));
                    this.$refs.annotationsFile.value = null;
                    return;
                }
                const filenameMapping = createFilenameMapping(cocoObject.images);
                const annotationsMapping = createAnnotationsMapping(cocoObject.annotations);
                const categoryMapping = createCategoryMapping(cocoObject.categories);
                this.objectsToUpload.addAnnotationsToAllObjects(filenameMapping, annotationsMapping, categoryMapping);
            };
            reader.readAsText(file);
        },
        changeAddAnnotations() {
            this.addAnnotations = !this.addAnnotations;
            this.objectsToUpload.removeAnnotationsFromAllObjects();
            this.$refs.annotationsFile.value = null;
        },
        // files upload ------------------------------------------------------------------------------------
        createObject() {
            if (!this.validateForm()) {
                return;
            }
            this.objectsToUpload.addMetadataToAllObjects(new ObjectMetadataCreateDTO({}, this.tags));
            if (this.doObjectDetection) {
                const odParams = new ObjectDetectionParametersDTO(this.odWantedLabels, this.odThreshold1, this.odThreshold2);
                this.objectsToUpload.addODParamsToAllObjects(odParams);
            }
            const res = this.objectsToUpload.uploadAllObjects();
            res.forEach(objResult => objResult.then(_ => this.updateObjects()))
            this.emptyForm();
        },
        validateForm() {
            if (this.objectsToUpload.getCount() <= 0) {
                alert("Select at least one object to upload.");
                return false;
            }
            if (!this.doObjectDetection) {
                return true;
            }
            if (Object.keys(this.odWantedLabels).length === 0) {
                alert("No labels to detect entered.");
                return false;
            }
            if (this.odThreshold1 < 0.0 || this.odThreshold1 > 1.0 ||
                this.odThreshold2 < 0.0 || this.odThreshold2 > 1.0) {
                alert("Thresholds for object detection must be between 0 and 1.");
                return false;
            }
            if (this.odThreshold1 > this.odThreshold2) {
                alert("Threshold 1 must be lower than or equal to Threshold 2.");
                return false;
            }
            return true;

        }
    },
    mounted() {
        this.updateObjects();
    }
}

Vue.createApp(SiteConfig).mount('#app')
