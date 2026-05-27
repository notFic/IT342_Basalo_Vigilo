import { API_BASE_URL, parseResponse, withRequesterEmail } from '../../core/apiClient'
import type { AuthResponse } from '../../core/types'

export async function loginUser(credentials: any): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(credentials),
  })
  return parseResponse<AuthResponse>(response)
}

export async function registerUser(userData: any, requesterEmail: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: withRequesterEmail(requesterEmail, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(userData),
  })
  return parseResponse<AuthResponse>(response)
}

export async function googleLoginUser(idToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  })
  return parseResponse<AuthResponse>(response)
}
