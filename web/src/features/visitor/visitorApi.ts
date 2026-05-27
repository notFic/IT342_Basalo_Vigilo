import { API_BASE_URL, parseResponse, withRequesterEmail } from '../../core/apiClient'
import type { PageResponse, VisitorLog } from '../../core/types'

export async function fetchActiveLogs(requesterEmail: string): Promise<VisitorLog[]> {
  const response = await fetch(`${API_BASE_URL}/logs/active`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<VisitorLog[]>(response)
}

export async function fetchHistoricalLogs(
  requesterEmail: string,
  page = 0,
  size = 50,
  query = '',
): Promise<PageResponse<VisitorLog>> {
  const search = query.trim() ? `&query=${encodeURIComponent(query.trim())}` : ''
  const response = await fetch(`${API_BASE_URL}/logs/history?page=${page}&size=${size}${search}`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<PageResponse<VisitorLog>>(response)
}

export async function createVisitorLog(payload: FormData, requesterEmail: string): Promise<VisitorLog> {
  const response = await fetch(`${API_BASE_URL}/logs/check-in`, {
    method: 'POST',
    headers: withRequesterEmail(requesterEmail),
    body: payload,
  })
  return parseResponse<VisitorLog>(response)
}

export async function checkOutVisitor(logId: number, requesterEmail: string): Promise<VisitorLog> {
  const response = await fetch(`${API_BASE_URL}/logs/${logId}/check-out`, {
    method: 'PUT',
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<VisitorLog>(response)
}

export async function voidVisitorLog(logId: number, requesterEmail: string, reason?: string): Promise<VisitorLog> {
  const query = reason && reason.trim()
    ? `?reason=${encodeURIComponent(reason.trim())}`
    : ''
  const response = await fetch(`${API_BASE_URL}/logs/${logId}/void${query}`, {
    method: 'PUT',
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<VisitorLog>(response)
}

export async function runAutoCloseJob(requesterEmail: string): Promise<{success: boolean; closedCount: number; message: string}> {
  const response = await fetch(`${API_BASE_URL}/logs/auto-close/run`, {
    method: 'POST',
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<{success: boolean; closedCount: number; message: string}>(response)
}
