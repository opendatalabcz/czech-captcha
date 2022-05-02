import {getLabelGroups, createLabelGroup} from "./api.js"

const SiteConfig = {
    data() {
        return {
            groupName: '',
            maxCardinality: 1,
            unlimitedRange: false,
            enteredLabel: "",
            labels: [],
            labelGroups: [],
        }
    },
    methods: {
        updateLabelGroups() {
            getLabelGroups().then(response => {
                this.labelGroups = response.data
            });
        },
        createLabelGroup() {
            if (!this.validFormInput()) {
                return
            }

            const labelGroup = {
                ...(!this.unlimitedRange && {labels: this.labels}),
                name: this.groupName,
                maxCardinality: this.maxCardinality
            }

            createLabelGroup(labelGroup).then(response => {
                this.updateLabelGroups()
                this.emptyForm()
            });
        },
        addLabel() {
            if (!!this.enteredLabel && !this.labels.includes(this.enteredLabel)) {
                this.labels.push(this.enteredLabel)
                this.enteredLabel = ""
            }
        },
        removeLabel(index) {
          if (this.labels.length > index) {
              this.labels.splice(index, 1);
          }
        },
        rangeDisplayValue(labelGroup) {
            return labelGroup.labelRange == null ? 'unlimited' : labelGroup.labelRange
        },
        emptyForm() {
            this.groupName = ""
            this.maxCardinality = 1
            this.enteredLabel = ""
            this.labels = []
        },
        validFormInput() {
            const validRange = this.unlimitedRange || this.labels.length > 1

            return this.maxCardinality >= 1 && !!this.groupName && validRange
        }
    },
    mounted() {
        this.updateLabelGroups();
    }
}

Vue.createApp(SiteConfig).mount('#app')
