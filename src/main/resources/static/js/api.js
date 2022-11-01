import {ENDPOINTS} from "./common.js";

function getConfigs() {
    return axios.get(ENDPOINTS.siteConfigs)
}

function deleteConfig(siteKey) {
    return axios.delete(ENDPOINTS.siteConfigs + "/" + siteKey)
}

function getTaskTypes() {
    return axios.get(ENDPOINTS.taskTypes)
}

function getTaskTypeSchema(taskType) {
    return axios.get(ENDPOINTS.taskTypeSchemas + "?taskName=" + taskType)
}

function createSiteConfig(configName, taskType, evaluationThreshold, generationConfig) {
    const body = {
        name: configName,
        taskConfig: {
            taskType,
            evaluationThreshold,
            generationConfig
        }
    }
    return axios.post(ENDPOINTS.siteConfigs, body)
}

function getLabelGroups() {
    return axios.get(ENDPOINTS.labelGroups)
}

function createLabelGroup(labelGroup) {
    return axios.post(ENDPOINTS.labelGroups, labelGroup)
}

function uploadUrlImage(urlImageDTO) {
    return axios.post(ENDPOINTS.urlImage, urlImageDTO)
}

function uploadUrlObject(urlObjectDTO) {
    return axios.post(ENDPOINTS.urlObject, urlObjectDTO)
}

function uploadFile(endpoint, file, fileDTOName, fileDTO) {
    let formData = new FormData();
    formData.append("file", file);
    formData.append(fileDTOName, new Blob([JSON.stringify(fileDTO)], {
        type: "application/json"
    }))
    return axios.post(endpoint, formData,
        {
            headers: { "Content-Type": "multipart/form-data" }
        });
}

function uploadFileImage(file, fileImageDTO) {
    return uploadFile(ENDPOINTS.fileImage, file, "fileImage", fileImageDTO);
}

function uploadFileObject(file, fileObjectDTO) {
    return uploadFile(ENDPOINTS.fileObject, file, "fileObject", fileObjectDTO);
}

function getObjects() {
    return axios.get(ENDPOINTS.objects)
}

export {getConfigs, deleteConfig, getTaskTypes, getTaskTypeSchema, createSiteConfig, getLabelGroups, createLabelGroup,
    getObjects, uploadUrlImage, uploadUrlObject, uploadFileImage, uploadFileObject}
