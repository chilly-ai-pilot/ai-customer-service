import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'
import App from './App.vue'

const app = createApp(App)

// 图标组件体积很小，unplugin-vue-components 默认不解析图标，这里手动全局注册即可，
// 不影响"Element Plus 组件按需自动导入"的要求（组件/样式已交给 unplugin-vue-components 处理）。
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)

app.mount('#app')
