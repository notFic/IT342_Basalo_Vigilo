export type AuthResponse = { success: boolean; data?: UserData; message: string }
export type UserData = { id: number; email: string; firstName: string; lastName: string; role: string }
export type VisitorLog = { id: number; fullName: string; contactNumber: string; hostName: string; visitorType: string; destinationRoom: string; purpose: string; extendedVisit: boolean; idImagePath: string; status: string; timeIn: string; timeOut: string | null }
export type Location = { id: number; areaName: string; roomNumber: string; floorLevel: string }
export type AuditLog = { id: number; timestamp: string; userEmail: string; actionPerformed: string; details: string }
export type Feedback = { type: 'success' | 'error' | ''; text: string }
