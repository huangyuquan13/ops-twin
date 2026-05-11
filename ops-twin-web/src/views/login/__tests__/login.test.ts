import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'

const store: Record<string, string> = {}

vi.stubGlobal('localStorage', {
  getItem(key: string) { return store[key] || null },
  setItem(key: string, value: string) { store[key] = value },
  removeItem(key: string) { delete store[key] },
  clear() { Object.keys(store).forEach(k => delete store[k]) }
})

describe('Login.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('renders login form', async () => {
    const LoginPage = (await import('@/views/login/index.vue')).default
    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), ElementPlus],
        stubs: { routerLink: true, routerView: true }
      }
    })
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('shows input fields', async () => {
    const LoginPage = (await import('@/views/login/index.vue')).default
    const wrapper = mount(LoginPage, {
      global: {
        plugins: [createPinia(), ElementPlus],
        stubs: { routerLink: true, routerView: true }
      }
    })
    const inputs = wrapper.findAll('input')
    expect(inputs.length).toBeGreaterThanOrEqual(2)
  })
})
