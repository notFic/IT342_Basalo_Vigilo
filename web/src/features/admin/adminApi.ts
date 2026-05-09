import { API_BASE_URL, parseResponse } from '../../core/apiClient'
import type { Location, UserData, AuditLog } from '../../core/types'

export async function fetchLocations(): Promise<Location[]> {
  const response = await fetch(`${API_BASE_URL}/locations`)
  return parseResponse<Location[]>(response)
}

export async function addLocation(payload: Omit<Location, 'id'>): Promise<Location> {
  const response = await fetch(`${API_BASE_URL}/locations`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
  })
  return parseResponse<Location>(response)
}

export async function fetchStaff(): Promise<UserData[]> {
  const response = await fetch(`${API_BASE_URL}/users`)
  return parseResponse<UserData[]>(response)
}

export async function fetchAuditLogs(): Promise<AuditLog[]> {
  const response = await fetch(`${API_BASE_URL}/audit`)
  return parseResponse<AuditLog[]>(response)
}
