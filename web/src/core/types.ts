export type AuthResponse = { success: boolean; data?: UserData | { user: UserData; accessToken: string }; message: string }
export type UserData = { id: number; email: string; firstName: string; lastName: string; role: string; createdAt?: string | null }
export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
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
  timeOut: string | null
  createdAt?: string | null
  updatedAt?: string | null
  createdByEmail?: string | null
  updatedByEmail?: string | null
  autoClosed?: boolean | null
}
export type Location = { id: number; areaName: string; roomNumber: string; floorLevel: string }
export type AuditLog = { id: number; timestamp: string; userEmail: string; actionPerformed: string; details: string }
export type AutoCloseSettings = {
  id: number
  enabled: boolean
  cutoffTime: string
  timezone: string
  lastRunDate?: string | null
  updatedAt?: string | null
  updatedByEmail?: string | null
}
export type HolidayStatus = {
  countryCode: string
  date: string
  isHoliday: boolean
  holidayName?: string
  holidayDate?: string
  message: string
  allHolidays?: Array<{ date: string; name: string }>
}
export type Feedback = { type: 'success' | 'error' | ''; text: string }

