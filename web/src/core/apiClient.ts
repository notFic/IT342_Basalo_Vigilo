export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type') ?? ''
  const isJson = contentType.includes('application/json')
  const body = isJson ? await response.json().catch(() => null) : await response.text()

  if (!response.ok) {
    const textBody = typeof body === 'string' ? body.trim() : ''
    const jsonMessage = typeof body === 'object' && body !== null && 'message' in body
      ? String(body.message ?? '').trim()
      : ''
    const message = textBody || jsonMessage || `Request failed (${response.status}).`
    throw new Error(message)
  }
  return body as T
}
