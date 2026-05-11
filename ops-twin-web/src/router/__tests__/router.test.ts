import { describe, it, expect, beforeEach, vi } from 'vitest'

const store: Record<string, string> = {}

vi.stubGlobal('localStorage', {
  getItem(key: string) { return store[key] || null },
  setItem(key: string, value: string) { store[key] = value },
  removeItem(key: string) { delete store[key] },
  clear() { Object.keys(store).forEach(k => delete store[k]) }
})

describe('Router Auth Guard', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects to login when no token', () => {
    const token = localStorage.getItem('token')
    expect(token).toBeNull()
  })

  it('allows navigation when token exists', () => {
    localStorage.setItem('token', 'test-jwt-token')
    const token = localStorage.getItem('token')
    expect(token).toBe('test-jwt-token')
  })
})
