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
};

function isTableRowOpened(openedRows, index) {
    return openedRows.has(index);
}

function toggleTableRow(openedRows, index) {
    if (isTableRowOpened(openedRows, index)) {
        openedRows.delete(index);
    } else {
        openedRows.add(index);
    }
}

function isEmptyObject(obj) {
    return obj
        && Object.keys(obj).length === 0
        && Object.getPrototypeOf(obj) === Object.prototype;
}

function formatToArrayString(array, emptyString) {
    if (Array.isArray(array)) {
        return array.length === 0 ? emptyString : array.join(", ");
    }
    return array;
}

export {ENDPOINTS, isTableRowOpened, toggleTableRow, isEmptyObject, formatToArrayString}
