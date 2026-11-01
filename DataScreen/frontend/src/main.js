import { createApp } from 'vue'
import { echarts } from './config/echarts'
import App from './App.vue'
import { dashboardTheme } from './config/dashboardTheme'
import './styles/main.css'
echarts.registerTheme('warehouse',dashboardTheme)
createApp(App).mount('#app')
