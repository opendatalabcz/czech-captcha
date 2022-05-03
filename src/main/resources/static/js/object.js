import {getObjects} from "./api.js"

const SiteConfig = {
    data() {
        return {
            objects: [],
            opened: new Set()
        }
    },
    methods: {
        updateObjects() {
            getObjects().then(response => {
                this.objects = response.data
                this.opened = new Set()
            });
        },
        toggleRow(index) {
            if (this.isOpened(index)) {
                this.opened.delete(index)
            } else {
                this.opened.add(index)
            }
        },
        isOpened(index) {
            return this.opened.has(index)
        }
    },
    mounted() {
        this.updateObjects();
    }
}

Vue.createApp(SiteConfig).mount('#app')
