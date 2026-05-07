import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
const service = axios.create({
    baseURL: 'http://localhost:8080', // 后端地址
    timeout: 5000
})
// 请求拦截器
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => Promise.reject(error)
)
// 响应拦截器
service.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '系统错误')
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('token')
            router.push('/login')
        }
        ElMessage.error('网络请求失败')
        return Promise.reject(error)
    }
)
export default service