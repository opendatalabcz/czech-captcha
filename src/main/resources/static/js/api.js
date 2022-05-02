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

export {getConfigs, deleteConfig, getTaskTypes, getTaskTypeSchema, createSiteConfig}
