import axios from 'axios'
const client=axios.create({baseURL:'/api',timeout:30000})
client.interceptors.response.use(r=>r.data,error=>Promise.reject(new Error(error.response?.data?.error?.message||'数据服务暂时不可用')))
export default client
