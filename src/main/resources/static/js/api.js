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
    return axios.get(ENDPOINTS.taskTypeSchemas + '?taskName=' + taskType)
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

export {getConfigs, deleteConfig, getTaskTypes, getTaskTypeSchema, createSiteConfig, getLabelGroups, createLabelGroup}
