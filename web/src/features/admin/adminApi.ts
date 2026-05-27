import { API_BASE_URL, parseResponse, withRequesterEmail } from '../../core/apiClient'
import type { Location, UserData, AuditLog } from '../../core/types'

export async function fetchLocations(requesterEmail: string): Promise<Location[]> {
  const response = await fetch(`${API_BASE_URL}/locations`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<Location[]>(response)
}

export async function addLocation(payload: Omit<Location, 'id'>, requesterEmail: string): Promise<Location> {
  const response = await fetch(`${API_BASE_URL}/locations`, {
    method: 'POST',
    headers: withRequesterEmail(requesterEmail, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  })
  return parseResponse<Location>(response)
}

export async function fetchStaff(requesterEmail: string): Promise<UserData[]> {
  const response = await fetch(`${API_BASE_URL}/users`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<UserData[]>(response)
}

export async function fetchAuditLogs(requesterEmail: string): Promise<AuditLog[]> {
  const response = await fetch(`${API_BASE_URL}/audit`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<AuditLog[]>(response)
}
