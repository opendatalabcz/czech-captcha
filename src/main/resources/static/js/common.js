const API_BASE_PATH = 'api/'
const OBJECTS_PATH = API_BASE_PATH + 'datamanagement/objects'
const TASKCONFIG_PATH = API_BASE_PATH + 'taskconfig'

const ENDPOINTS = {
    siteConfigs: API_BASE_PATH + 'siteconfig',
    labelGroups: OBJECTS_PATH + '/labelgroups',
    urlObject: OBJECTS_PATH + '/url',
    urlImage: OBJECTS_PATH + '/image/url',
    fileObject: OBJECTS_PATH + '/file',
    fileImage: OBJECTS_PATH + '/image/file',
    objects: OBJECTS_PATH,
    taskTypes: TASKCONFIG_PATH + '/tasks',
    taskTypeSchemas: TASKCONFIG_PATH + '/schemas'
}


export {ENDPOINTS}
