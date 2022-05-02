import {getConfigs, deleteConfig, getTaskTypes, getTaskTypeSchema, createSiteConfig} from "./api.js"

const SiteConfig = {
    data() {
        return {
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
        updateConfigList() {
            getConfigs().then(response => {
                this.configs = response.data
            });
        },
        deleteConfig(siteKey) {
            deleteConfig(siteKey).then(response => {
                this.updateConfigList()
            });
        },
        createSiteConfig() {
            const generationConfig = this.generationConfigEditor.getValue()

            // console.log(this.configName, this.evaluationThreshold, this.taskType)
            createSiteConfig(this.configName, this.taskType, this.evaluationThreshold, generationConfig).then(response => {
                this.updateConfigList()
            });
        },

        updateTaskTypes() {
            getTaskTypes().then(response => {
                this.taskTypes = response.data
                this.taskType = this.taskTypes[0]
                console.log(this.taskTypes, this.taskType)
                // this.updateTaskSchema()
            })
        },
        updateTaskSchema() {
            getTaskTypeSchema(this.taskType).then(response => {
                this.taskSchema = response.data
                console.log(response.data)

                if (this.generationConfigEditor != null) {
                    this.generationConfigEditor.destroy()
                }
                // todo set up the form so that it looks a bit better
                const generationFormElem = document.getElementById('generationConfig')
                const options = {
                    schema: this.taskSchema,
                    disable_edit_json: true,
                    disable_properties: true,
                    disable_collapse: true,
                    enable_array_copy: true,
                    // theme: 'bootstrap4'
                    iconlib: "fontawesome5"
                }
                this.generationConfigEditor = new JSONEditor(generationFormElem, options);
            });
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

Vue.createApp(SiteConfig).mount('#app')
