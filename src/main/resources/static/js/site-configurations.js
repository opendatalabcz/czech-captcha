import {getConfigs, deleteConfig, getTaskTypes, getTaskTypeSchema, createSiteConfig} from "./api.js";
import {isTableRowOpened, toggleTableRow, isEmptyObject, formatToArrayString} from "./common.js";

const SiteConfigurations = {
    data() {
        return {
            showNewModal: false,
            openedRows: new Set(),

            configName: '',
            taskType: '',
            evaluationThreshold: 1,
            configs: [],
            taskTypes: [],
            taskSchema: {},
            generationConfigEditor: null
        }
    },
    methods: {
        toggleRow(index) { toggleTableRow(this.openedRows, index); },
        isRowOpened(index) { return isTableRowOpened(this.openedRows, index); },
        isEmptyObject(obj) { return isEmptyObject(obj); },
        displayArray(array) { return formatToArrayString(array, "\u2205"); },
        updateConfigList() {
            getConfigs().then(response => {
                this.configs = response.data;
                this.openedRows = new Set();
            });
        },
        deleteConfig(siteKey) {
            deleteConfig(siteKey).then(_ => {
                this.updateConfigList()
            });
        },
        createSiteConfig() {
            if (!this.validFormInput()) {
                return
            }
            const generationConfig = this.generationConfigEditor.getValue()

            createSiteConfig(this.configName, this.taskType, this.evaluationThreshold, generationConfig).then(_ => {
                this.updateConfigList()
            });
            this.emptyForm()
        },

        updateTaskTypes() {
            getTaskTypes().then(response => {
                this.taskTypes = response.data
                this.taskType = this.taskTypes[0]
            })
        },
        updateTaskSchema() {
            getTaskTypeSchema(this.taskType).then(response => {
                this.taskSchema = response.data

                if (this.generationConfigEditor != null) {
                    this.generationConfigEditor.destroy()
                }
                const generationFormElem = document.getElementById('generationConfig')
                const options = {
                    schema: this.taskSchema,
                    disable_edit_json: true,
                    disable_properties: true,
                    disable_array_reorder: true,
                    disable_collapse: true,
                    show_errors: 'change',
                    theme: 'bootstrap4'
                }
                this.generationConfigEditor = new JSONEditor(generationFormElem, options);
            });
        },
        emptyForm() {
            this.showNewModal = false;
            this.configName = '';
            this.evaluationThreshold = 1;
            this.taskType = this.taskTypes[0];
            this.generationConfigEditor.setValue({});
        },
        validFormInput() {
            if (!this.configName) {
                alert("Enter site configuration name.");
                return false;
            }
            if (this.evaluationThreshold < 0 || this.evaluationThreshold > 1) {
                alert("Evaluation threshold must be between 0 and 1.");
                return false;
            }
            const validateTask = this.generationConfigEditor.validate();
            if (validateTask.length !== 0) {
                let message = "Task configuration is not valid: ";
                for (const error of validateTask) {
                    message += "\n" + error.path + ": " + error.message;
                }
                alert(message);
                return false;
            }
            return true;
        }
    },
    watch: {
        taskType: function(newTaskType) {
            this.taskType = newTaskType
            this.updateTaskSchema()
        }
    },
    mounted() {
        this.updateConfigList();
        this.updateTaskTypes();
    }
}

Vue.createApp(SiteConfigurations).mount('#app')
