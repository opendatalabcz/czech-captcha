const API_BASE_PATH = 'api/'
const OBJECTS_PATH = API_BASE_PATH + 'datamanagement/objects'
const TASKCONFIG_PATH = API_BASE_PATH + 'taskconfig'

const ENDPOINTS = {
    siteConfigs: API_BASE_PATH + 'siteconfig',
    labelGroups: OBJECTS_PATH + '/labelgroups',
    createUrlObject: OBJECTS_PATH + '/url',
    objects: OBJECTS_PATH + 'TODO! create endpoint summarizing the user\'s objects',
    taskTypes: TASKCONFIG_PATH + '/tasks',
    taskTypeSchemas: TASKCONFIG_PATH + '/schemas'
}


export {ENDPOINTS}
