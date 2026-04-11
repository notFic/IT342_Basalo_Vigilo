const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

type ApiErrorPayload = {
  message?: string
}

async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type') ?? ''
  const isJson = contentType.includes('application/json')
  const body = isJson ? await response.json() : await response.text()

  if (!response.ok) {
    const message =
      typeof body === 'string'
        ? body
        : (body as ApiErrorPayload)?.message ?? 'Request failed.'
    throw new Error(message)
  }

  return body as T
}

export type LoginPayload = {
  email: string
  password: string
}

export type RegisterPayload = {
  firstName: string
  lastName: string
  email: string
  password: string
  role: string
}

export type UserData = {
  email: string
  firstName: string
  lastName: string
  role: string
}

export type AuthResponse = {
  success: boolean
  message: string
  data?: UserData
}

export type VisitorLog = {
  id: number
  fullName: string
  contactNumber: string
  hostName: string
  visitorType: string
  destinationRoom: string
  purpose: string
  extendedVisit: boolean
  idImagePath: string
  status: string
  timeIn: string
  timeOut?: string | null
  createdByEmail: string
  updatedByEmail?: string | null
}

export async function registerUser(payload: RegisterPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  return parseResponse<AuthResponse>(response)
}

export async function loginUser(payload: LoginPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  return parseResponse<AuthResponse>(response)
}

export async function fetchActiveLogs(): Promise<VisitorLog[]> {
  const response = await fetch(`${API_BASE_URL}/logs/active`)
  return parseResponse<VisitorLog[]>(response)
}

export async function createVisitorLog(formData: FormData): Promise<VisitorLog> {
  const response = await fetch(`${API_BASE_URL}/logs/check-in`, {
    method: 'POST',
    body: formData,
  })

  return parseResponse<VisitorLog>(response)
}

export async function checkOutVisitor(logId: number, updatedByEmail: string): Promise<VisitorLog> {
  const query = new URLSearchParams({ updatedByEmail }).toString()
  const response = await fetch(`${API_BASE_URL}/logs/${logId}/check-out?${query}`, {
    method: 'PUT',
  })

  return parseResponse<VisitorLog>(response)
}
