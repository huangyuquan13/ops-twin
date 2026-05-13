import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const localUserStr = localStorage.getItem('userInfo')
  const userInfo = ref(localUserStr ? JSON.parse(localUserStr) : {
    id: null,
    username: '未登录',
    avatar: '',
    roleId: null
  })

  // 按钮级权限码列表，如 ["strategy:add", "strategy:execute", ...]
  const savedPerms = localStorage.getItem('userPermissions')
  const savedMenus = localStorage.getItem('userMenus')
  const permissions = ref<string[]>(savedPerms ? JSON.parse(savedPerms) : [])
  // 菜单树（type=1 的权限），供侧边栏动态渲染
  const menus = ref<any[]>(savedMenus ? JSON.parse(savedMenus) : [])

  // 按 parentId 分组：{ 0: [section1, section2], 1: [item1, item2] }
  const menuSections = computed(() => {
    const map: Record<number, any[]> = {}
    menus.value.forEach((m: any) => {
      const pid = m.parentId || 0
      if (!map[pid]) map[pid] = []
      map[pid].push(m)
    })
    return map
  })

  const isAdmin = computed(() => userInfo.value.roleId === 1)
  const avatarUrl = computed(() => {
    if (!userInfo.value.avatar) return 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
    return `${import.meta.env.VITE_API_BASE}${userInfo.value.avatar}`
  })

  // 检查是否拥有某个按钮权限
  const hasPerm = (code: string): boolean => {
    return permissions.value.includes(code)
  }

  const setUserInfo = (data: any) => {
    userInfo.value = data
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  const setPermissions = (perms: string[]) => {
    permissions.value = perms
    localStorage.setItem('userPermissions', JSON.stringify(perms))
  }

  const setMenus = (data: any[]) => {
    menus.value = data
    localStorage.setItem('userMenus', JSON.stringify(data))
  }

  const updateAvatar = (newAvatar: string) => {
    userInfo.value.avatar = newAvatar
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const clearUserInfo = () => {
    userInfo.value = { id: null, username: '未登录', avatar: '', roleId: null }
    permissions.value = []
    menus.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userMenus')
    localStorage.removeItem('userPermissions')
  }

  return {
    userInfo,
    permissions,
    menus,
    menuSections,
    isAdmin,
    avatarUrl,
    hasPerm,
    setUserInfo,
    setPermissions,
    setMenus,
    updateAvatar,
    clearUserInfo
  }
})
