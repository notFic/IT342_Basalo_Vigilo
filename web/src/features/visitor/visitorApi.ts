import { API_BASE_URL, parseResponse } from '../../core/apiClient'
import type { VisitorLog } from '../../core/types'

export async function fetchActiveLogs(): Promise<VisitorLog[]> {
  const response = await fetch(`${API_BASE_URL}/logs/active`)
  return parseResponse<VisitorLog[]>(response)
}

export async function fetchHistoricalLogs(page = 0, size = 50): Promise<{content: VisitorLog[]}> {
  const response = await fetch(`${API_BASE_URL}/logs/history?page=${page}&size=${size}`)
  return parseResponse<{content: VisitorLog[]}>(response)
}

export async function createVisitorLog(payload: FormData): Promise<VisitorLog> {
  const response = await fetch(`${API_BASE_URL}/logs/check-in`, { method: 'POST', body: payload })
  return parseResponse<VisitorLog>(response)
}

export async function checkOutVisitor(logId: number, userEmail: string): Promise<VisitorLog> {
  const response = await fetch(`${API_BASE_URL}/logs/${logId}/check-out?updatedByEmail=${encodeURIComponent(userEmail)}`, { method: 'PUT' })
  return parseResponse<VisitorLog>(response)
}

export async function voidVisitorLog(logId: number, adminEmail: string): Promise<VisitorLog> {
  const response = await fetch(`${API_BASE_URL}/logs/${logId}/void?updatedByEmail=${encodeURIComponent(adminEmail)}`, { method: 'PUT' })
  return parseResponse<VisitorLog>(response)
}
