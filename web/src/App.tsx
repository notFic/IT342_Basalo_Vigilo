import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { loginUser, registerUser, googleLoginUser } from './features/auth/authApi'
import {
  fetchActiveLogs,
  fetchHistoricalLogs,
  createVisitorLog,
  checkOutVisitor,
  voidVisitorLog,
  runAutoCloseJob,
} from './features/visitor/visitorApi'
import { fetchStaff, fetchLocations, addLocation, fetchAuditLogs } from './features/admin/adminApi'
import { fetchAutoCloseSettings, fetchHolidayStatus, updateAutoCloseSettings } from './features/system/systemApi'
import { useGoogleLogin } from '@react-oauth/google'
import type {
  AuthResponse,
  VisitorLog,
  UserData,
  Location,
  AuditLog,
  Feedback,
  AutoCloseSettings,
  HolidayStatus,
} from './core/types'
import { AUTH_STORAGE_KEY, AUTH_TOKEN_KEY, isAdminRole, normalizeRole, normalizeUser } from './core/auth'

type Tab = 'active' | 'history' | 'staff' | 'locations' | 'audit' | 'settings'
type SortDirection = 'asc' | 'desc'
type SortConfig = { key: string; direction: SortDirection }

type LoginForm = {
  email: string
  password: string
}

type VisitorForm = {
  fullName: string
  contactNumber: string
  hostName: string
  visitorType: string
  destinationRoom: string
  purpose: string
  extendedVisit: boolean
  idImage: File | null
}

const PAGE_SIZE = 10

const initialLoginForm: LoginForm = {
  email: '',
  password: '',
}

const initialVisitorForm: VisitorForm = {
  fullName: '',
  contactNumber: '',
  hostName: '',
  visitorType: 'Guest',
  destinationRoom: '',
  purpose: '',
  extendedVisit: false,
  idImage: null,
}

const initialPageState: Record<Tab, number> = {
  active: 1,
  history: 1,
  staff: 1,
  locations: 1,
  audit: 1,
  settings: 1,
}

const initialSortState: Record<Tab, SortConfig | null> = {
  active: { key: 'timeIn', direction: 'desc' },
  history: { key: 'timeIn', direction: 'desc' },
  staff: { key: 'createdAt', direction: 'desc' },
  locations: { key: 'roomNumber', direction: 'asc' },
  audit: { key: 'timestamp', direction: 'desc' },
  settings: null,
}

function App() {
  const [showPassword, setShowPassword] = useState(false)
  const [loginForm, setLoginForm] = useState<LoginForm>(initialLoginForm)
  const [visitorForm, setVisitorForm] = useState<VisitorForm>(initialVisitorForm)
  const [authFeedback, setAuthFeedback] = useState<Feedback>({ type: '', text: '' })
  const [dashboardFeedback, setDashboardFeedback] = useState<Feedback>({ type: '', text: '' })
  const [user, setUser] = useState<AuthResponse['data'] | null>(null)
  const [activeLogs, setActiveLogs] = useState<VisitorLog[]>([])
  const [historicalLogs, setHistoricalLogs] = useState<VisitorLog[]>([])
  const [staffList, setStaffList] = useState<UserData[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
  const [autoCloseSettings, setAutoCloseSettings] = useState<AutoCloseSettings | null>(null)
  const [holidayStatus, setHolidayStatus] = useState<HolidayStatus | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedLog, setSelectedLog] = useState<VisitorLog | null>(null)
  const [activeTab, setActiveTab] = useState<Tab>('active')
  const [pageState, setPageState] = useState<Record<Tab, number>>(initialPageState)
  const [sortState, setSortState] = useState<Record<Tab, SortConfig | null>>(initialSortState)
  const [isStaffModalOpen, setIsStaffModalOpen] = useState(false)
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false)
  const [isCheckInModalOpen, setIsCheckInModalOpen] = useState(false)
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false)
  const [isAuthSubmitting, setIsAuthSubmitting] = useState(false)
  const [isVisitorSubmitting, setIsVisitorSubmitting] = useState(false)
  const [isLogsLoading, setIsLogsLoading] = useState(false)
  const [isStaffSubmitting, setIsStaffSubmitting] = useState(false)
  const [isLocationSubmitting, setIsLocationSubmitting] = useState(false)
  const [isSettingsSubmitting, setIsSettingsSubmitting] = useState(false)
  const [isHolidaysModalOpen, setIsHolidaysModalOpen] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(false)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', isDarkMode ? 'dark' : 'light')
  }, [isDarkMode])

  useEffect(() => {
    const storedUser = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!storedUser) return
    const parsed = JSON.parse(storedUser) as AuthResponse['data']
    setUser(parsed ? normalizeUser(parsed) : null)
  }, [])

  useEffect(() => {
    if (!user?.email) return
    void loadHolidayStatus()
  }, [user?.email])

  useEffect(() => {
    if (!user?.email) return

    if (activeTab === 'active') void loadActiveLogs()
    if (activeTab === 'history') void loadHistoricalLogs()
    if (activeTab === 'staff') void loadStaff()
    if (activeTab === 'locations') void loadLocations()
    if (activeTab === 'audit') void loadAuditLogs()
    if (activeTab === 'settings') void loadAutoCloseSettings()

    if (locations.length === 0) void loadLocations()
  }, [activeTab, user?.email, locations.length])

  useEffect(() => {
    setPageState((current) => ({ ...current, [activeTab]: 1 }))
  }, [searchQuery, activeTab])

  useEffect(() => {
    if (isHolidaysModalOpen) {
      setTimeout(() => {
        document.getElementById('upcoming-holiday')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 100);
    }
  }, [isHolidaysModalOpen])

  async function loadHolidayStatus() {
    if (!user?.email) return
    try {
      setHolidayStatus(await fetchHolidayStatus(user.email))
    } catch {
      setHolidayStatus({
        countryCode: 'PH',
        date: new Date().toISOString().slice(0, 10),
        isHoliday: false,
        message: 'Normal Access Day (PH)',
      })
    }
  }

  async function loadActiveLogs() {
    if (!user?.email) return
    setIsLogsLoading(true)
    try {
      setActiveLogs(await fetchActiveLogs(user.email))
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load active logs.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  async function loadHistoricalLogs() {
    if (!user?.email) return
    setIsLogsLoading(true)
    try {
      const page = await fetchHistoricalLogs(user.email, 0, 500, '')
      setHistoricalLogs(page.content)
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load historical logs.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  async function loadStaff() {
    if (!user?.email) return
    setIsLogsLoading(true)
    try {
      setStaffList(await fetchStaff(user.email))
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load staff accounts.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  async function loadLocations() {
    if (!user?.email) return
    setIsLogsLoading(true)
    try {
      setLocations(await fetchLocations(user.email))
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load locations.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  async function loadAuditLogs() {
    if (!user?.email || !isAdminRole(user.role)) return
    setIsLogsLoading(true)
    try {
      setAuditLogs(await fetchAuditLogs(user.email))
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load audit logs.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  async function loadAutoCloseSettings() {
    if (!user?.email) return
    setIsLogsLoading(true)
    try {
      setAutoCloseSettings(await fetchAutoCloseSettings(user.email))
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load auto-close settings.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  const handleLoginChange = (field: keyof LoginForm, value: string) => {
    setLoginForm((current) => ({ ...current, [field]: value }))
  }

  const handleVisitorChange = (field: keyof VisitorForm, value: string | boolean | File | null) => {
    setVisitorForm((current) => ({ ...current, [field]: value }))
  }

  const handleLogin = async (event: FormEvent) => {
    event.preventDefault()
    setAuthFeedback({ type: '', text: '' })
    setIsAuthSubmitting(true)

    try {
      const response = await loginUser(loginForm)
      if (!response.success || !response.data) {
        throw new Error(response.message || 'Login failed.')
      }

      const loginData = response.data as any
      const userData = 'user' in loginData ? loginData.user : loginData
      const token = 'accessToken' in loginData ? loginData.accessToken : null
      
      const normalizedUser = normalizeUser(userData)
      setUser(normalizedUser)
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(normalizedUser))
      if (token) {
         localStorage.setItem(AUTH_TOKEN_KEY, token)
      }
      setAuthFeedback({ type: 'success', text: response.message })
      setLoginForm(initialLoginForm)
      setActiveTab('active')
    } catch (error) {
      setAuthFeedback({
        type: 'error',
        text: error instanceof Error && error.message.trim() ? error.message : 'Login failed.',
      })
    } finally {
      setIsAuthSubmitting(false)
    }
  }

  const handleGoogleLogin = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      setAuthFeedback({ type: '', text: '' })
      setIsAuthSubmitting(true)
      try {
        const accessToken = tokenResponse.access_token
        const data = await googleLoginUser(accessToken)
        if (data.success) {
          const loginData = data.data as any
          const userData = 'user' in loginData ? loginData.user : loginData
          const token = 'accessToken' in loginData ? loginData.accessToken : null
          const normalizedUser = normalizeUser(userData)
          setUser(normalizedUser)
          localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(normalizedUser))
          if (token) localStorage.setItem(AUTH_TOKEN_KEY, token)
          setAuthFeedback({ type: 'success', text: data.message })
          setActiveTab('active')
        } else {
          setAuthFeedback({ type: 'error', text: data.message || 'Google login failed.' })
        }
      } catch (error: any) {
        setAuthFeedback({ type: 'error', text: error?.message || 'Unable to connect to backend for Google verification.' })
      } finally {
        setIsAuthSubmitting(false)
      }
    },
    onError: () => {
      setAuthFeedback({ type: 'error', text: 'Google Sign-In failed.' })
    },
  })

  const validateVisitorForm = () => {
    if (!visitorForm.fullName || !visitorForm.contactNumber || !visitorForm.hostName || !visitorForm.destinationRoom || !visitorForm.purpose) {
      return 'Complete all required visitor fields.'
    }
    if (!/^\d{7,15}$/.test(visitorForm.contactNumber)) {
      return 'Contact number must contain 7 to 15 digits only.'
    }
    if (!visitorForm.idImage) {
      return 'Upload the visitor ID image before submitting.'
    }
    return ''
  }

  const handleVisitorSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const validation = validateVisitorForm()
    if (validation) {
      setDashboardFeedback({ type: 'error', text: validation })
      return
    }
    if (!user?.email) {
      setDashboardFeedback({ type: 'error', text: 'Log in first before submitting a visitor entry.' })
      return
    }

    const payload = new FormData()
    payload.append('fullName', visitorForm.fullName)
    payload.append('contactNumber', visitorForm.contactNumber)
    payload.append('hostName', visitorForm.hostName)
    payload.append('visitorType', visitorForm.visitorType)
    payload.append('destinationRoom', visitorForm.destinationRoom)
    payload.append('purpose', visitorForm.purpose)
    payload.append('extendedVisit', String(visitorForm.extendedVisit))
    payload.append('idImage', visitorForm.idImage as File)

    setIsVisitorSubmitting(true)
    setDashboardFeedback({ type: '', text: '' })
    try {
      await createVisitorLog(payload, user.email)
      setVisitorForm(initialVisitorForm)
      setDashboardFeedback({ type: 'success', text: 'Visitor check-in saved successfully.' })
      setIsCheckInModalOpen(false)
      await loadActiveLogs()
      await loadAuditLogs()
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to save visitor entry.',
      })
    } finally {
      setIsVisitorSubmitting(false)
    }
  }

  const handleCheckOut = async (logId: number) => {
    if (!user?.email) return
    try {
      await checkOutVisitor(logId, user.email)
      setDashboardFeedback({ type: 'success', text: 'Visitor checked out successfully.' })
      await loadActiveLogs()
      await loadHistoricalLogs()
      await loadAuditLogs()
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to process check-out.',
      })
    }
  }

  const handleVoid = async (logId: number) => {
    if (!user?.email) return
    const reason = window.prompt('Optional void reason for the audit trail and notification email:', '')
    if (reason === null) return

    try {
      await voidVisitorLog(logId, user.email, reason)
      setDashboardFeedback({ type: 'success', text: 'Record voided successfully.' })
      setSelectedLog(null)
      await loadActiveLogs()
      await loadHistoricalLogs()
      await loadAuditLogs()
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to void record.',
      })
    }
  }

  const handleRunAutoClose = async () => {
    if (!user?.email || !isAdminRole(user.role)) return
    try {
      const result = await runAutoCloseJob(user.email)
      setDashboardFeedback({
        type: 'success',
        text: `${result.message} Closed ${result.closedCount} active record${result.closedCount === 1 ? '' : 's'}.`,
      })
      await loadActiveLogs()
      await loadHistoricalLogs()
      await loadAuditLogs()
      await loadAutoCloseSettings()
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to run auto-close job.',
      })
    }
  }

  const handleSaveAutoCloseSettings = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!user?.email || !isAdminRole(user.role) || !autoCloseSettings) return

    const form = event.currentTarget as typeof event.currentTarget & {
      enabled: { checked: boolean }
      cutoffTime: { value: string }
      timezone: { value: string }
    }

    setIsSettingsSubmitting(true)
    try {
      const updated = await updateAutoCloseSettings(user.email, {
        enabled: form.enabled.checked,
        cutoffTime: form.cutoffTime.value,
        timezone: form.timezone.value,
      })
      setAutoCloseSettings(updated)
      setDashboardFeedback({ type: 'success', text: 'Auto-close settings updated successfully.' })
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to update auto-close settings.',
      })
    } finally {
      setIsSettingsSubmitting(false)
    }
  }

  const handleLogout = () => {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    localStorage.removeItem(AUTH_TOKEN_KEY)
    setUser(null)
    setActiveLogs([])
    setHistoricalLogs([])
    setStaffList([])
    setLocations([])
    setAuditLogs([])
    setAutoCloseSettings(null)
    setDashboardFeedback({ type: '', text: '' })
    setAuthFeedback({ type: '', text: '' })
    setIsLogoutModalOpen(false)
  }

  const setTab = (tab: Tab) => {
    setActiveTab(tab)
    setSearchQuery('')
  }

  const toggleSort = (tab: Tab, key: string) => {
    setSortState((current) => {
      const existing = current[tab]
      const nextDirection: SortDirection = existing?.key === key && existing.direction === 'asc' ? 'desc' : 'asc'
      return { ...current, [tab]: { key, direction: nextDirection } }
    })
  }

  const setTabPage = (tab: Tab, page: number) => {
    setPageState((current) => ({ ...current, [tab]: page }))
  }

  const getSortIndicator = (tab: Tab, key: string) => {
    const sort = sortState[tab]
    if (!sort || sort.key !== key) return <span className="sort-indicator">↕</span>
    return sort.direction === 'asc' ? <span className="sort-indicator sort-active">↑</span> : <span className="sort-indicator sort-active">↓</span>
  }

  const matchesSearch = (tab: Tab, item: VisitorLog | UserData | Location | AuditLog) => {
    const search = searchQuery.trim().toLowerCase()
    if (!search) return true

    if (tab === 'active' || tab === 'history') {
      const log = item as VisitorLog
      return [
        log.fullName,
        log.hostName,
        log.destinationRoom,
        log.visitorType,
        log.status,
      ].some((value) => String(value ?? '').toLowerCase().includes(search))
    }

    if (tab === 'staff') {
      const staff = item as UserData
      return [
        `${staff.firstName} ${staff.lastName}`,
        staff.email,
        staff.role,
      ].some((value) => String(value ?? '').toLowerCase().includes(search))
    }

    if (tab === 'locations') {
      const location = item as Location
      return [location.areaName, location.roomNumber, location.floorLevel]
        .some((value) => String(value ?? '').toLowerCase().includes(search))
    }

    const audit = item as AuditLog
    return [audit.userEmail, audit.actionPerformed, audit.details]
      .some((value) => String(value ?? '').toLowerCase().includes(search))
  }

  const sortItems = <T extends object>(items: T[], sort: SortConfig | null): T[] => {
    if (!sort) return [...items]

    return [...items].sort((left, right) => {
      const leftValue = getComparableValue((left as Record<string, unknown>)[sort.key])
      const rightValue = getComparableValue((right as Record<string, unknown>)[sort.key])

      if (leftValue < rightValue) return sort.direction === 'asc' ? -1 : 1
      if (leftValue > rightValue) return sort.direction === 'asc' ? 1 : -1
      return 0
    })
  }

  const paginate = <T,>(items: T[], page: number) => {
    const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE))
    const safePage = Math.min(Math.max(page, 1), totalPages)
    const start = (safePage - 1) * PAGE_SIZE
    const end = start + PAGE_SIZE
    return {
      totalPages,
      page: safePage,
      pageItems: items.slice(start, end),
      start: items.length === 0 ? 0 : start + 1,
      end: Math.min(end, items.length),
    }
  }

  const activeData = sortItems(activeLogs.filter((item) => matchesSearch('active', item)), sortState.active)
  const historyData = sortItems(historicalLogs.filter((item) => matchesSearch('history', item)), sortState.history)
  const staffData = sortItems(staffList.filter((item) => matchesSearch('staff', item)), sortState.staff)
  const locationData = sortItems(locations.filter((item) => matchesSearch('locations', item)), sortState.locations)
  const auditData = sortItems(auditLogs.filter((item) => matchesSearch('audit', item)), sortState.audit)

  const activePage = paginate(activeData, pageState.active)
  const historyPage = paginate(historyData, pageState.history)
  const staffPage = paginate(staffData, pageState.staff)
  const locationPage = paginate(locationData, pageState.locations)
  const auditPage = paginate(auditData, pageState.audit)

  const exportCurrentTabAsCsv = () => {
    if (!user || !isAdminRole(user.role)) return
    const rows: string[][] = []
    let fileName = `${activeTab}.csv`

    if (activeTab === 'active') {
      fileName = 'vigilo-active-logs.csv'
      rows.push(['Visitor Name', 'Contact Number', 'Host Name', 'Visitor Type', 'Destination/Room', 'Purpose', 'Extended Visit', 'Time In', 'Status', 'Created By'])
      activeData.forEach((item) => rows.push([
        item.fullName,
        item.contactNumber,
        item.hostName,
        item.visitorType,
        item.destinationRoom,
        item.purpose,
        String(item.extendedVisit),
        item.timeIn,
        item.status,
        item.createdByEmail ?? '',
      ]))
    } else if (activeTab === 'history') {
      fileName = 'vigilo-historical-logs.csv'
      rows.push(['Visitor Name', 'Contact Number', 'Host Name', 'Visitor Type', 'Destination/Room', 'Purpose', 'Extended Visit', 'Time In', 'Time Out', 'Status', 'Created By', 'Updated By', 'Auto Closed'])
      historyData.forEach((item) => rows.push([
        item.fullName,
        item.contactNumber,
        item.hostName,
        item.visitorType,
        item.destinationRoom,
        item.purpose,
        String(item.extendedVisit),
        item.timeIn,
        item.timeOut ?? '',
        item.status,
        item.createdByEmail ?? '',
        item.updatedByEmail ?? '',
        String(item.autoClosed ?? false),
      ]))
    } else if (activeTab === 'staff') {
      fileName = 'vigilo-staff.csv'
      rows.push(['First Name', 'Last Name', 'Email', 'Role', 'Created At'])
      staffData.forEach((item) => rows.push([item.firstName, item.lastName, item.email, item.role, item.createdAt ?? '']))
    } else if (activeTab === 'locations') {
      fileName = 'vigilo-locations.csv'
      rows.push(['Area Name', 'Room Number', 'Floor Level'])
      locationData.forEach((item) => rows.push([item.areaName, item.roomNumber, item.floorLevel]))
    } else if (activeTab === 'audit') {
      fileName = 'vigilo-audit-log.csv'
      rows.push(['Timestamp', 'User', 'Action', 'Details'])
      auditData.forEach((item) => rows.push([item.timestamp, item.userEmail, item.actionPerformed, item.details]))
    } else {
      return
    }

    const csv = rows
      .map((row) => row.map((value) => `"${String(value ?? '').replace(/"/g, '""')}"`).join(','))
      .join('\n')

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()
    URL.revokeObjectURL(url)
  }

  if (!user) {
    return (
      <div className="auth-shell">
        <div className="auth-card" style={{ position: 'relative' }}>
          <button 
            className="theme-toggle" 
            style={{ position: 'absolute', top: '16px', right: '16px' }}
            onClick={() => setIsDarkMode(!isDarkMode)} 
            title="Toggle Dark Mode"
            type="button"
          >
            {isDarkMode ? '☀️' : '🌙'}
          </button>
          <div className="auth-header">
            <div className="auth-logo">V</div>
            <h1>Vigilo</h1>
            <p>Sign in to your account to continue</p>
          </div>

          {authFeedback.text && <div className={`feedback-alert ${authFeedback.type}`}>{authFeedback.text}</div>}

          <form onSubmit={handleLogin} className="auth-form">
            <label>
              <span>Email Address</span>
              <input type="email" value={loginForm.email} onChange={(event) => handleLoginChange('email', event.target.value)} required />
            </label>
            <label>
              <div className="label-row">
                <span>Password</span>
                <a href="#" className="forgot-link">Forgot Password?</a>
              </div>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={loginForm.password}
                  onChange={(event) => handleLoginChange('password', event.target.value)}
                  required
                  style={{ width: '100%', paddingRight: '70px' }}
                />
                <button type="button" onClick={() => setShowPassword(!showPassword)} style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-main)' }} tabIndex={-1}>
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </label>
            <button type="submit" className="primary-btn full-width" disabled={isAuthSubmitting}>
              {isAuthSubmitting ? <span className="btn-progress"><span className="spinner" />Signing In...</span> : 'Login'}
            </button>
            <div className="divider"><span>or continue with</span></div>
            <button type="button" className="secondary-btn full-width" onClick={() => handleGoogleLogin()} disabled={isAuthSubmitting}>
              <svg style={{ width: '18px', height: '18px', marginRight: '8px', verticalAlign: 'middle' }} viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              Sign in with Google
            </button>
          </form>
        </div>
      </div>
    )
  }

  const currentCount = activeTab === 'active'
    ? activeData.length
    : activeTab === 'history'
      ? historyData.length
      : activeTab === 'staff'
        ? staffData.length
        : activeTab === 'locations'
          ? locationData.length
          : activeTab === 'audit'
            ? auditData.length
            : 0

  const renderSortableHeader = (tab: Tab, label: string, key: string) => (
    <button type="button" className="sort-button" onClick={() => toggleSort(tab, key)} title={`Sort by ${label}`}>
      <span>{label}</span>
      {getSortIndicator(tab, key)}
    </button>
  )

  const renderPagination = (tab: Tab, pageInfo: { page: number; totalPages: number; start: number; end: number }, totalItems: number, position: 'top' | 'bottom') => {
    if (position === 'top' && totalItems <= 10) return null;
    if (totalItems === 0) return null;
    return (
      <div className={position === 'top' ? 'table-pagination-top' : 'table-pagination-bottom'}>
        <span>Showing <b>{pageInfo.start}-{pageInfo.end}</b> of <b>{totalItems}</b> records</span>
        <div className="pagination-controls">
          <button className="page-btn" type="button" disabled={pageInfo.page <= 1} onClick={() => setTabPage(tab, pageInfo.page - 1)}>← Previous</button>
          <span>Page {pageInfo.page} of {pageInfo.totalPages}</span>
          <button className="page-btn" type="button" disabled={pageInfo.page >= pageInfo.totalPages} onClick={() => setTabPage(tab, pageInfo.page + 1)}>Next →</button>
        </div>
      </div>
    )
  }

  return (
    <div className="app-container">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-logo">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="7 3 12 21 17 3"></polyline></svg>
          </div>
          <h2>Vigilo</h2>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-group">
            <span className="nav-title">Visitor Management</span>
            <button className={`nav-item ${activeTab === 'active' ? 'active' : ''}`} onClick={() => setTab('active')}>Active Logs</button>
            <button className={`nav-item ${activeTab === 'history' ? 'active' : ''}`} onClick={() => setTab('history')}>Historical Logs</button>
          </div>

          {isAdminRole(user.role) && (
            <div className="nav-group">
              <span className="nav-title">System</span>
              <button className={`nav-item ${activeTab === 'staff' ? 'active' : ''}`} onClick={() => setTab('staff')}>Staff Management</button>
              <button className={`nav-item ${activeTab === 'locations' ? 'active' : ''}`} onClick={() => setTab('locations')}>Facility Locations</button>
              <button className={`nav-item ${activeTab === 'audit' ? 'active' : ''}`} onClick={() => setTab('audit')}>System Audit Log</button>
              <button className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setTab('settings')}>Auto-Close Settings</button>
            </div>
          )}
        </nav>
      </aside>

      <div className="main-content">
        <header className="top-header">
          <button className={`api-banner ${holidayStatus?.isHoliday ? 'holiday' : 'active'} clickable-banner`} onClick={() => setIsHolidaysModalOpen(true)}>
            <div className="banner-content">
              <strong>{holidayStatus?.message ?? 'Checking holiday status...'}</strong>
            </div>
          </button>
          <div className="header-toolbar">
            <input type="text" className="search-bar" placeholder={activeTab === 'settings' ? 'Search disabled for settings' : 'Search current table...'} value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} disabled={activeTab === 'settings'} />
            <div className="header-actions">
              <button className="theme-toggle" onClick={() => setIsDarkMode(!isDarkMode)} title="Toggle Dark Mode">
                {isDarkMode ? '☀️' : '🌙'}
              </button>
              <button className="user-profile clickable-profile" onClick={() => setIsLogoutModalOpen(true)}>
                <span className={`role-badge ${isAdminRole(user.role) ? 'admin' : ''}`}>{isAdminRole(user.role) ? 'Admin' : 'Staff'} Badge</span>
                <div className="user-details">
                  <span className="user-name">{user.firstName} {user.lastName}</span>
                  <span className="user-title">{normalizeRole(user.role)}</span>
                </div>
                <span className="dropdown-icon">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
                </span>
              </button>
            </div>
          </div>
        </header>

        <main className="dashboard-body">
          <div className="page-header">
            <div>
              <h1>
                {activeTab === 'active' ? 'Active Logs'
                  : activeTab === 'history' ? 'Historical Logs'
                    : activeTab === 'staff' ? 'Staff Management'
                      : activeTab === 'locations' ? 'Location Management'
                        : activeTab === 'audit' ? 'System Audit Log'
                          : 'Auto-Close Settings'}
              </h1>
              <p>
                {activeTab === 'active' ? `Currently monitoring ${activeLogs.length} active visitors.`
                  : activeTab === 'history' ? 'Viewing past visitor records.'
                    : activeTab === 'staff' ? 'Manage system accounts and roles.'
                      : activeTab === 'locations' ? 'Manage valid rooms and areas.'
                        : activeTab === 'audit' ? 'Timeline of system events.'
                          : 'Configure and manually trigger the nightly visitor auto-close process.'}
              </p>
            </div>

            <div className="page-actions">
              {activeTab !== 'settings' && isAdminRole(user.role) && <button className="text-btn" onClick={exportCurrentTabAsCsv}>↓ Export CSV</button>}
              {activeTab === 'active' && <button className="primary-btn" onClick={() => setIsCheckInModalOpen(true)}>New Visitor Check-in</button>}
              {activeTab === 'staff' && <button className="primary-btn" onClick={() => setIsStaffModalOpen(true)}>Register New Staff</button>}
              {activeTab === 'locations' && <button className="primary-btn" onClick={() => setIsLocationModalOpen(true)}>Add Location</button>}
            </div>
          </div>

          {dashboardFeedback.text && <div className={`feedback-alert ${dashboardFeedback.type}`}>{dashboardFeedback.text}</div>}
          {(isLogsLoading || isVisitorSubmitting || isStaffSubmitting || isLocationSubmitting || isSettingsSubmitting) && (
            <div className="page-loading-bar" aria-hidden="true">
              <span />
            </div>
          )}

          {activeTab === 'settings' ? (
            <div className="settings-container">
              <div className="settings-info-panel">
                <h3>Automated Nightly Cleanup</h3>
                <p>The Auto-Close job automatically identifies visitor logs that are still active past the designated cutoff time. It updates their status to <strong>"Auto-Closed"</strong> to prevent stale logs from accumulating on the dashboard.</p>
                <div className="info-alert">
                  <strong>Exemption Rule:</strong> Visitors marked as <em>"Extended Visit"</em> are strictly exempt from this process and will remain active until manually checked out by a staff member.
                </div>
              </div>

              <div className="settings-card">
                <form className="settings-form" onSubmit={handleSaveAutoCloseSettings}>
                  <div className="form-group-box">
                    <label className="toggle-container">
                      <span className="toggle-label">Enable Scheduled Cleanup</span>
                      <label className="toggle-switch">
                        <input type="checkbox" name="enabled" defaultChecked={autoCloseSettings?.enabled ?? true} />
                        <span className="slider"></span>
                      </label>
                      <small>If disabled, no records will be automatically closed.</small>
                    </label>
                  </div>
                  
                  <div className="split-row">
                    <label>
                      <span>Daily Cutoff Time</span>
                      <input type="time" name="cutoffTime" defaultValue={autoCloseSettings?.cutoffTime ?? '23:59'} required />
                    </label>
                    <label>
                      <span>System Timezone</span>
                      <input type="text" name="timezone" defaultValue={autoCloseSettings?.timezone ?? 'Asia/Manila'} required />
                    </label>
                  </div>

                  <div className="settings-meta-bar">
                    <span><strong>Last Run Date:</strong> {autoCloseSettings?.lastRunDate ?? 'Never'}</span>
                    <span><strong>Last Updated By:</strong> {autoCloseSettings?.updatedByEmail ?? '-'}</span>
                  </div>

                  <div className="modal-actions">
                    <button type="button" className="text-btn" onClick={handleRunAutoClose}>▶ Run Auto-Close Manually Now</button>
                    <button type="submit" className="primary-btn" disabled={isSettingsSubmitting}>
                      {isSettingsSubmitting ? <span className="btn-progress"><span className="spinner" />Saving...</span> : 'Save Configuration'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          ) : (
            <div className="table-container">
              {activeTab === 'active' && renderPagination('active', activePage, activeData.length, 'top')}
              {activeTab === 'history' && renderPagination('history', historyPage, historyData.length, 'top')}
              {activeTab === 'staff' && renderPagination('staff', staffPage, staffData.length, 'top')}
              {activeTab === 'locations' && renderPagination('locations', locationPage, locationData.length, 'top')}
              {activeTab === 'audit' && renderPagination('audit', auditPage, auditData.length, 'top')}
              <table className="data-table">
                {activeTab === 'staff' ? (
                  <>
                    <thead>
                      <tr>
                        <th>{renderSortableHeader('staff', 'Full Name', 'firstName')}</th>
                        <th>{renderSortableHeader('staff', 'Email Address', 'email')}</th>
                        <th>{renderSortableHeader('staff', 'Role', 'role')}</th>
                        <th>{renderSortableHeader('staff', 'Date Created', 'createdAt')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {staffPage.pageItems.map((staff, index) => (
                        <tr key={`${staff.email}-${index}`}>
                          <td>{staff.firstName} {staff.lastName}</td>
                          <td>{staff.email}</td>
                          <td>{normalizeRole(staff.role)}</td>
                          <td>{staff.createdAt ? new Date(staff.createdAt).toLocaleString() : '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </>
                ) : activeTab === 'locations' ? (
                  <>
                    <thead>
                      <tr>
                        <th>{renderSortableHeader('locations', 'Area Name', 'areaName')}</th>
                        <th>{renderSortableHeader('locations', 'Room Number', 'roomNumber')}</th>
                        <th>{renderSortableHeader('locations', 'Floor Level', 'floorLevel')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {locationPage.pageItems.map((location, index) => (
                        <tr key={`${location.id}-${index}`}>
                          <td>{location.areaName}</td>
                          <td>{location.roomNumber}</td>
                          <td>{location.floorLevel}</td>
                        </tr>
                      ))}
                    </tbody>
                  </>
                ) : activeTab === 'audit' ? (
                  <>
                    <thead>
                      <tr>
                        <th>{renderSortableHeader('audit', 'Timestamp', 'timestamp')}</th>
                        <th>{renderSortableHeader('audit', 'User', 'userEmail')}</th>
                        <th>{renderSortableHeader('audit', 'Action Performed', 'actionPerformed')}</th>
                        <th>{renderSortableHeader('audit', 'Details', 'details')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {auditPage.pageItems.map((log, index) => (
                        <tr key={`${log.id}-${index}`}>
                          <td>{new Date(log.timestamp).toLocaleString()}</td>
                          <td>{log.userEmail}</td>
                          <td>{log.actionPerformed}</td>
                          <td>{log.details}</td>
                        </tr>
                      ))}
                    </tbody>
                  </>
                ) : (
                  <>
                    <thead>
                      <tr>
                        <th>{renderSortableHeader(activeTab, 'Visitor Name', 'fullName')}</th>
                        <th>{renderSortableHeader(activeTab, 'Type', 'visitorType')}</th>
                        <th>{renderSortableHeader(activeTab, 'Host (Tenant)', 'hostName')}</th>
                        <th>{renderSortableHeader(activeTab, 'Destination/Room', 'destinationRoom')}</th>
                        {activeTab === 'active' ? <th>{renderSortableHeader('active', 'Time In', 'timeIn')}</th> : <th>{renderSortableHeader('history', 'Time Out', 'timeOut')}</th>}
                        {activeTab === 'history' && <th>{renderSortableHeader('history', 'Status', 'status')}</th>}
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(activeTab === 'active' ? activePage.pageItems : historyPage.pageItems).map((log) => (
                        <tr key={log.id} onClick={() => setSelectedLog(log)} style={{ cursor: 'pointer' }} className="clickable-row">
                          <td>{log.fullName}{log.extendedVisit && <span className="extended-tag">Extended</span>}</td>
                          <td><span className="type-box">{log.visitorType}</span></td>
                          <td>{log.hostName}</td>
                          <td>{log.destinationRoom}</td>
                          {activeTab === 'active' ? (
                            <td>{new Date(log.timeIn).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
                          ) : (
                            <td>{log.timeOut ? new Date(log.timeOut).toLocaleString() : '-'}</td>
                          )}
                          {activeTab === 'history' && <td>{log.status}</td>}
                          <td>
                            {activeTab === 'active' && (
                              <button className="action-btn" onClick={(event) => { event.stopPropagation(); handleCheckOut(log.id) }}>
                                Check out
                              </button>
                            )}
                            {activeTab === 'history' && isAdminRole(user.role) && log.status !== 'Voided' && (
                              <button className="action-btn void-btn" onClick={(event) => { event.stopPropagation(); handleVoid(log.id) }}>
                                Void
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </>
                )}
              </table>

              {activeTab === 'active' && renderPagination('active', activePage, activeData.length, 'bottom')}
              {activeTab === 'history' && renderPagination('history', historyPage, historyData.length, 'bottom')}
              {activeTab === 'staff' && renderPagination('staff', staffPage, staffData.length, 'bottom')}
              {activeTab === 'locations' && renderPagination('locations', locationPage, locationData.length, 'bottom')}
              {activeTab === 'audit' && renderPagination('audit', auditPage, auditData.length, 'bottom')}

              {currentCount === 0 && !isLogsLoading && (
                <div className="empty-state">No records found.</div>
              )}
            </div>
          )}
        </main>
      </div>

      {isCheckInModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content check-in-modal">
            <h2>New Visitor Entry</h2>
            <form onSubmit={handleVisitorSubmit} className="modal-form">
              <div className="split-row">
                <label><span>Full Name</span><input value={visitorForm.fullName} onChange={(event) => handleVisitorChange('fullName', event.target.value)} placeholder="John Doe" required /></label>
                <label><span>Contact Number</span><input value={visitorForm.contactNumber} onChange={(event) => handleVisitorChange('contactNumber', event.target.value)} placeholder="09171234567" required /></label>
              </div>
              <div className="form-group-box">
                <div className="split-row">
                  <label><span>Host Name (Tenant)</span><input value={visitorForm.hostName} onChange={(event) => handleVisitorChange('hostName', event.target.value)} placeholder="Jane Doe" required /></label>
                  <label><span>Destination/Room</span>
                    <select value={visitorForm.destinationRoom} onChange={(event) => handleVisitorChange('destinationRoom', event.target.value)} required>
                      <option value="" disabled>Select Location</option>
                      {locations.map((location, index) => (
                        <option key={index} value={`${location.roomNumber} - ${location.areaName}`}>{location.roomNumber} - {location.areaName}</option>
                      ))}
                    </select>
                  </label>
                </div>
              </div>
              <div className="split-row align-center">
                <label>
                  <span>Visitor Type</span>
                  <select value={visitorForm.visitorType} onChange={(event) => handleVisitorChange('visitorType', event.target.value)}>
                    <option value="Guest">Guest</option>
                    <option value="Employee">Employee</option>
                    <option value="Contractor">Contractor</option>
                    <option value="Courier">Courier</option>
                  </select>
                </label>
                <div className="toggle-container">
                  <label><span>Mark as Extended Visit</span>
                    <div className="toggle-switch">
                      <input type="checkbox" checked={visitorForm.extendedVisit} onChange={(event) => handleVisitorChange('extendedVisit', event.target.checked)} />
                      <span className="slider"></span>
                    </div>
                  </label>
                  <small>Exempts this visitor from the automated cutoff cleanup.</small>
                </div>
              </div>
              <label>
                <span>Purpose of Visit</span>
                <textarea rows={3} value={visitorForm.purpose} onChange={(event) => handleVisitorChange('purpose', event.target.value)} required></textarea>
              </label>
              <label>
                <span>Visitor ID Attachment</span>
                <input type="file" accept="image/*" className="file-input" onChange={(event) => handleVisitorChange('idImage', event.target.files?.[0] ?? null)} required />
              </label>
              <div className="modal-actions">
                <button type="button" className="secondary-btn" onClick={() => setIsCheckInModalOpen(false)}>Cancel</button>
                <button type="submit" className="primary-btn" disabled={isVisitorSubmitting}>
                  {isVisitorSubmitting ? <span className="btn-progress"><span className="spinner" />Submitting...</span> : 'Submit Check-in'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isLogoutModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content logout-modal">
            <div className="logout-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/></svg>
            </div>
            <h2>Ready to Leave?</h2>
            <p>Are you sure you want to log out of your session?<br />You will need to sign in again to access the dashboard.</p>
            <div className="modal-actions-center">
              <button className="secondary-btn" onClick={() => setIsLogoutModalOpen(false)}>Cancel</button>
              <button className="primary-btn" onClick={handleLogout}>Logout</button>
            </div>
          </div>
        </div>
      )}

      {isHolidaysModalOpen && (
        <div className="modal-overlay" onClick={() => setIsHolidaysModalOpen(false)}>
          <div className="modal-content holidays-modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header-icon">📅</div>
            <h2>Public Holidays (PH)</h2>
            <div className="holidays-list">
              {holidayStatus?.allHolidays && holidayStatus.allHolidays.length > 0 ? (
                (() => {
                  let firstUpcomingFound = false;
                  return holidayStatus.allHolidays.map((h, i) => {
                    const isPassed = new Date(h.date).getTime() < new Date().setHours(0, 0, 0, 0);
                    let id = undefined;
                    if (!isPassed && !firstUpcomingFound) {
                      firstUpcomingFound = true;
                      id = "upcoming-holiday";
                    }
                    return (
                      <div key={i} id={id} className={`holiday-item ${isPassed ? 'holiday-passed' : ''}`}>
                        <span className="holiday-date">{new Date(h.date).toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric'})}</span>
                        <span className="holiday-name">{h.name}</span>
                      </div>
                    );
                  });
                })()
              ) : (
                <p>No holidays found.</p>
              )}
            </div>
            <div className="modal-actions-center">
              <button className="secondary-btn" onClick={() => setIsHolidaysModalOpen(false)}>Close</button>
            </div>
          </div>
        </div>
      )}

      {isStaffModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Register New Staff</h2>
            <form onSubmit={async (event) => {
              event.preventDefault()
              if (!user?.email) return

              const form = event.currentTarget as typeof event.currentTarget & {
                firstName: { value: string }
                lastName: { value: string }
                email: { value: string }
                password: { value: string }
                role: { value: string }
              }

              setIsStaffSubmitting(true)
              try {
                await registerUser({
                  firstName: form.firstName.value,
                  lastName: form.lastName.value,
                  email: form.email.value,
                  password: form.password.value,
                  role: form.role.value,
                }, user.email)
                setIsStaffModalOpen(false)
                await loadStaff()
                await loadAuditLogs()
                setDashboardFeedback({ type: 'success', text: 'Staff account created successfully.' })
              } catch (error) {
                setDashboardFeedback({
                  type: 'error',
                  text: error instanceof Error ? error.message : 'Unable to create staff account.',
                })
              } finally {
                setIsStaffSubmitting(false)
              }
            }}>
              <div className="form-group"><label>First Name</label><input name="firstName" required /></div>
              <div className="form-group"><label>Last Name</label><input name="lastName" required /></div>
              <div className="form-group"><label>Email</label><input name="email" required type="email" /></div>
              <div className="form-group"><label>Password</label><input name="password" required type="password" /></div>
              <div className="form-group"><label>Role</label><select name="role"><option value="STAFF">STAFF</option><option value="ADMIN">ADMIN</option></select></div>
              <div className="modal-actions">
                <button type="button" className="secondary-btn" onClick={() => setIsStaffModalOpen(false)}>Cancel</button>
                <button type="submit" className="primary-btn" disabled={isStaffSubmitting}>
                  {isStaffSubmitting ? <span className="btn-progress"><span className="spinner" />Registering...</span> : 'Register User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isLocationModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Add Location</h2>
            <form onSubmit={async (event) => {
              event.preventDefault()
              if (!user?.email) return

              const form = event.currentTarget as typeof event.currentTarget & {
                areaName: { value: string }
                roomNumber: { value: string }
                floorLevel: { value: string }
              }

              setIsLocationSubmitting(true)
              try {
                await addLocation({
                  areaName: form.areaName.value,
                  roomNumber: form.roomNumber.value,
                  floorLevel: form.floorLevel.value,
                }, user.email)
                setIsLocationModalOpen(false)
                await loadLocations()
                setDashboardFeedback({ type: 'success', text: 'Location added successfully.' })
              } catch (error) {
                setDashboardFeedback({
                  type: 'error',
                  text: error instanceof Error ? error.message : 'Unable to add location.',
                })
              } finally {
                setIsLocationSubmitting(false)
              }
            }}>
              <div className="form-group"><label>Area Name</label><input name="areaName" required /></div>
              <div className="form-group"><label>Room Number</label><input name="roomNumber" required /></div>
              <div className="form-group"><label>Floor Level</label><input name="floorLevel" required /></div>
              <div className="modal-actions">
                <button type="button" className="secondary-btn" onClick={() => setIsLocationModalOpen(false)}>Cancel</button>
                <button type="submit" className="primary-btn" disabled={isLocationSubmitting}>
                  {isLocationSubmitting ? <span className="btn-progress"><span className="spinner" />Saving...</span> : 'Add Location'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {selectedLog && (
        <div className="modal-overlay" onClick={() => setSelectedLog(null)}>
          <div className="modal-content" onClick={(event) => event.stopPropagation()}>
            <h2>Visitor Details</h2>
            <div className="visitor-details-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginTop: '15px' }}>
              <div className="detail-field"><strong>Full Name:</strong><br />{selectedLog.fullName}</div>
              <div className="detail-field"><strong>Contact:</strong><br />{selectedLog.contactNumber}</div>
              <div className="detail-field"><strong>Host:</strong><br />{selectedLog.hostName}</div>
              <div className="detail-field"><strong>Room:</strong><br />{selectedLog.destinationRoom}</div>
              <div className="detail-field"><strong>Type:</strong><br />{selectedLog.visitorType}</div>
              <div className="detail-field"><strong>Purpose:</strong><br />{selectedLog.purpose}</div>
              <div className="detail-field"><strong>Time In:</strong><br />{new Date(selectedLog.timeIn).toLocaleString()}</div>
              <div className="detail-field"><strong>Time Out:</strong><br />{selectedLog.timeOut ? new Date(selectedLog.timeOut).toLocaleString() : '-'}</div>
              <div className="detail-field"><strong>Status:</strong><br />{selectedLog.status}</div>
              <div className="detail-field"><strong>Created By:</strong><br />{selectedLog.createdByEmail ?? '-'}</div>
              <div className="detail-field"><strong>Updated By:</strong><br />{selectedLog.updatedByEmail ?? '-'}</div>
            </div>
            {selectedLog.idImagePath && (
              <div className="visitor-image-container" style={{ marginTop: '20px', textAlign: 'center' }}>
                <p style={{ marginBottom: '10px' }}><strong>ID Image:</strong></p>
                <img src={`${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'}/logs/images/${selectedLog.idImagePath}`} alt="Visitor ID" style={{ maxWidth: '100%', maxHeight: '300px', borderRadius: '8px', border: '1px solid #ccc' }} />
              </div>
            )}
            <div className="modal-actions" style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end' }}>
              {isAdminRole(user.role) && selectedLog.status !== 'Voided' && (
                <button type="button" className="action-btn void-btn" onClick={() => handleVoid(selectedLog.id)}>Void</button>
              )}
              <button type="button" className="secondary-btn" onClick={() => setSelectedLog(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function getComparableValue(value: unknown): string | number {
  if (value === null || value === undefined) return ''
  if (typeof value === 'boolean') return value ? 1 : 0
  if (typeof value === 'number') return value
  const stringValue = String(value).trim()
  const timestamp = Date.parse(stringValue)
  if (!Number.isNaN(timestamp) && /^\d{4}-\d{2}-\d{2}/.test(stringValue)) {
    return timestamp
  }
  return stringValue.toLowerCase()
}

export default App



