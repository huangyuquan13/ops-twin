import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 【内聚点 1：自动初始化】初始化时，直接尝试从硬盘读取。如果没有，才给默认值。
  // 这样 Layout 里就再也不用写 onMounted 去读硬盘了！
  const localUserStr = localStorage.getItem('userInfo')
  const userInfo = ref(localUserStr ? JSON.parse(localUserStr) : {
    id: null,
    username: '未登录',
    avatar: '',
    roleId: null
  })

  // 【Getters】派生状态
  const isAdmin = computed(() => userInfo.value.roleId === 1)
  const avatarUrl = computed(() => {
    if (!userInfo.value.avatar) return 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
    return `http://localhost:8080${userInfo.value.avatar}`
  })

  // 【内聚点 2：自动存储】设置用户信息，并同步写回硬盘
  const setUserInfo = (data: any) => {
    userInfo.value = data
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  // 【内聚点 3：自动更新】更新头像，并同步写回硬盘
  const updateAvatar = (newAvatar: string) => {
    userInfo.value.avatar = newAvatar
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  // 【内聚点 4：一键清理】清除用户信息，并扫地出门（删 token 和 本地数据）
  const clearUserInfo = () => {
    userInfo.value = { id: null, username: '未登录', avatar: '', roleId: null }
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    userInfo,
    isAdmin,
    avatarUrl,
    setUserInfo,
    updateAvatar,
    clearUserInfo
  }
})
