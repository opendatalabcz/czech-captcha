import {getLabelGroups, createLabelGroup} from "./api.js"
import {formatToArrayString, isTableRowOpened, toggleTableRow} from "./common.js";

const SiteConfig = {
    data() {
        return {
            openedRows: new Set(),
            showNewModal: false,

            groupName: '',
            maxCardinality: 1,
            enteredLabel: "",
            labels: [],
            labelGroups: [],
        }
    },
    methods: {
        toggleRow(index) { toggleTableRow(this.openedRows, index); },
        isRowOpened(index) { return isTableRowOpened(this.openedRows, index); },
        rangeDisplayCount(labelGroup) {
            return labelGroup.labelRange == null ? "unlimited" : labelGroup.labelRange.length;
        },
        rangeDisplayValue(labelGroup) {
            return labelGroup.labelRange == null ? "unlimited" : formatToArrayString(labelGroup.labelRange, "\u2205");
        },
        updateLabelGroups() {
            getLabelGroups().then(response => {
                this.labelGroups = response.data;
                this.openedRows = new Set();
            });
        },
        createLabelGroup() {
            if (!this.validFormInput()) {
                return
            }

            const labelGroup = {
                labels: this.labels,
                name: this.groupName,
                maxCardinality: this.maxCardinality
            }

            createLabelGroup(labelGroup).then(_ => {
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
        emptyForm() {
            this.showNewModal = false;
            this.groupName = ""
            this.maxCardinality = 1
            this.enteredLabel = ""
            this.labels = []
        },
        validFormInput() {
            if (!this.groupName) {
                alert("Enter label group name.");
                return false;
            }
            if (this.labelGroups.some(group => group.name === this.groupName)) {
                alert("Label group with name " + this.groupName + " already exists.");
                return false;
            }
            if (this.maxCardinality < 1) {
                alert("Maximal cardinality of a label group must be at least 1.");
                return false;
            }
            return true;
        }
    },
    mounted() {
        this.updateLabelGroups();
    }
}

Vue.createApp(SiteConfig).mount('#app')
