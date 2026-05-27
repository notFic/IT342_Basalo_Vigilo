import { API_BASE_URL, parseResponse, withRequesterEmail } from '../../core/apiClient'
import type { AutoCloseSettings, HolidayStatus } from '../../core/types'

export async function fetchHolidayStatus(requesterEmail: string): Promise<HolidayStatus> {
  const response = await fetch(`${API_BASE_URL}/system/holiday-status`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<HolidayStatus>(response)
}

export async function fetchAutoCloseSettings(requesterEmail: string): Promise<AutoCloseSettings> {
  const response = await fetch(`${API_BASE_URL}/settings/auto-close`, {
    headers: withRequesterEmail(requesterEmail),
  })
  return parseResponse<AutoCloseSettings>(response)
}

export async function updateAutoCloseSettings(
  requesterEmail: string,
  payload: Partial<AutoCloseSettings>,
): Promise<AutoCloseSettings> {
  const response = await fetch(`${API_BASE_URL}/settings/auto-close`, {
    method: 'PUT',
    headers: withRequesterEmail(requesterEmail, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  })
  return parseResponse<AutoCloseSettings>(response)
}
