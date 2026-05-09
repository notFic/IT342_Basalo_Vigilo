import type { UserData } from './types'

export function normalizeRole(role: string | null | undefined): string {
  const normalized = (role ?? '').trim().toUpperCase()
  if (normalized.includes('ADMIN')) return 'ADMIN'
  if (normalized.includes('STAFF')) return 'STAFF'
  return normalized || 'STAFF'
}

export function isAdminRole(role: string | null | undefined): boolean {
  return normalizeRole(role) === 'ADMIN'
}

export function normalizeUser(user: UserData): UserData {
  return { ...user, role: normalizeRole(user.role) }
}
