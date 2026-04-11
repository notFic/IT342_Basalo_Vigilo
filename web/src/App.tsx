import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import {
  checkOutVisitor,
  createVisitorLog,
  fetchActiveLogs,
  loginUser,
  registerUser,
} from './api'
import type { AuthResponse, VisitorLog } from './api'

type Mode = 'login' | 'register'

type Feedback = {
  type: 'success' | 'error' | ''
  text: string
}

type AuthForm = {
  firstName: string
  lastName: string
  email: string
  password: string
  role: string
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

const initialAuthForm: AuthForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  role: 'STAFF',
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

const storageKey = 'vigilo-user'

function App() {
  const [mode, setMode] = useState<Mode>('login')
  const [authForm, setAuthForm] = useState<AuthForm>(initialAuthForm)
  const [visitorForm, setVisitorForm] = useState<VisitorForm>(initialVisitorForm)
  const [authFeedback, setAuthFeedback] = useState<Feedback>({ type: '', text: '' })
  const [dashboardFeedback, setDashboardFeedback] = useState<Feedback>({ type: '', text: '' })
  const [user, setUser] = useState<AuthResponse['data'] | null>(null)
  const [activeLogs, setActiveLogs] = useState<VisitorLog[]>([])
  const [isAuthSubmitting, setIsAuthSubmitting] = useState(false)
  const [isVisitorSubmitting, setIsVisitorSubmitting] = useState(false)
  const [isLogsLoading, setIsLogsLoading] = useState(false)

  useEffect(() => {
    const storedUser = localStorage.getItem(storageKey)
    if (storedUser) {
      setUser(JSON.parse(storedUser))
    }
  }, [])

  useEffect(() => {
    if (user) {
      void loadActiveLogs()
    }
  }, [user])

  async function loadActiveLogs() {
    setIsLogsLoading(true)
    try {
      const logs = await fetchActiveLogs()
      setActiveLogs(logs)
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load active logs.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

  const welcomeName = useMemo(() => {
    if (!user) return ''
    return `${user.firstName} ${user.lastName}`.trim()
  }, [user])

  const handleAuthChange = (field: keyof AuthForm, value: string) => {
    setAuthForm((current) => ({ ...current, [field]: value }))
  }

  const handleVisitorChange = (field: keyof VisitorForm, value: string | boolean | File | null) => {
    setVisitorForm((current) => ({ ...current, [field]: value }))
  }

  const handleRegister = async (event: FormEvent) => {
    event.preventDefault()
    setAuthFeedback({ type: '', text: '' })
    setIsAuthSubmitting(true)

    try {
      const response = await registerUser(authForm)
      setAuthFeedback({ type: 'success', text: response.message })
      setMode('login')
      setAuthForm((current) => ({ ...current, password: '' }))
    } catch (error) {
      setAuthFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Registration failed.',
      })
    } finally {
      setIsAuthSubmitting(false)
    }
  }

  const handleLogin = async (event: FormEvent) => {
    event.preventDefault()
    setAuthFeedback({ type: '', text: '' })
    setIsAuthSubmitting(true)

    try {
      const response = await loginUser({
        email: authForm.email,
        password: authForm.password,
      })

      setAuthFeedback({ type: 'success', text: response.message })
      setUser(response.data ?? null)
      localStorage.setItem(storageKey, JSON.stringify(response.data ?? null))
      setAuthForm(initialAuthForm)
    } catch (error) {
      setAuthFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Login failed.',
      })
    } finally {
      setIsAuthSubmitting(false)
    }
  }

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
    payload.append('createdByEmail', user.email)
    payload.append('idImage', visitorForm.idImage as File)

    setIsVisitorSubmitting(true)
    setDashboardFeedback({ type: '', text: '' })

    try {
      await createVisitorLog(payload)
      setVisitorForm(initialVisitorForm)
      setDashboardFeedback({ type: 'success', text: 'Visitor check-in saved successfully.' })
      await loadActiveLogs()
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
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to process check-out.',
      })
    }
  }

  const handleLogout = () => {
    localStorage.removeItem(storageKey)
    setUser(null)
    setActiveLogs([])
    setDashboardFeedback({ type: '', text: '' })
  }

  if (!user) {
    return (
      <div className="auth-shell">
        <div className="auth-card">
          <div className="brand-block">
            <span className="eyebrow">Vigilo</span>
            <h1>{mode === 'login' ? 'Guard Sign-In' : 'Register Staff Account'}</h1>
            <p className="copy">
              {mode === 'login'
                ? 'Access the visitor dashboard to check in guests, monitor active logs, and process departures.'
                : 'Create a staff account connected to the Spring Boot backend and Supabase PostgreSQL database.'}
            </p>
          </div>

          <div className="mode-switch">
            <button className={mode === 'login' ? 'mode active' : 'mode'} type="button" onClick={() => setMode('login')}>
              Login
            </button>
            <button className={mode === 'register' ? 'mode active' : 'mode'} type="button" onClick={() => setMode('register')}>
              Register
            </button>
          </div>

          {mode === 'login' ? (
            <form className="stack" onSubmit={handleLogin}>
              <label className="field">
                <span>Email Address</span>
                <input
                  type="email"
                  value={authForm.email}
                  onChange={(event) => handleAuthChange('email', event.target.value)}
                  placeholder="guard@example.com"
                  required
                />
              </label>
              <label className="field">
                <span>Password</span>
                <input
                  type="password"
                  value={authForm.password}
                  onChange={(event) => handleAuthChange('password', event.target.value)}
                  placeholder="Enter your password"
                  required
                />
              </label>
              {authFeedback.text && <div className={`feedback ${authFeedback.type}`}>{authFeedback.text}</div>}
              <button className="primary-button" type="submit" disabled={isAuthSubmitting}>
                {isAuthSubmitting ? 'Signing In...' : 'Login'}
              </button>
            </form>
          ) : (
            <form className="stack" onSubmit={handleRegister}>
              <div className="split-fields">
                <label className="field">
                  <span>First Name</span>
                  <input
                    value={authForm.firstName}
                    onChange={(event) => handleAuthChange('firstName', event.target.value)}
                    placeholder="Kurt"
                    required
                  />
                </label>
                <label className="field">
                  <span>Last Name</span>
                  <input
                    value={authForm.lastName}
                    onChange={(event) => handleAuthChange('lastName', event.target.value)}
                    placeholder="Basalo"
                    required
                  />
                </label>
              </div>
              <label className="field">
                <span>Email Address</span>
                <input
                  type="email"
                  value={authForm.email}
                  onChange={(event) => handleAuthChange('email', event.target.value)}
                  placeholder="guard@example.com"
                  required
                />
              </label>
              <label className="field">
                <span>Password</span>
                <input
                  type="password"
                  value={authForm.password}
                  onChange={(event) => handleAuthChange('password', event.target.value)}
                  placeholder="At least 8 characters"
                  required
                />
              </label>
              <label className="field">
                <span>Role</span>
                <select value={authForm.role} onChange={(event) => handleAuthChange('role', event.target.value)}>
                  <option value="STAFF">Staff</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </label>
              {authFeedback.text && <div className={`feedback ${authFeedback.type}`}>{authFeedback.text}</div>}
              <button className="primary-button" type="submit" disabled={isAuthSubmitting}>
                {isAuthSubmitting ? 'Registering...' : 'Create Account'}
              </button>
            </form>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="dashboard-shell">
      <aside className="sidebar">
        <div>
          <span className="eyebrow">Vigilo</span>
          <h1>Active Logs Dashboard</h1>
          <p className="copy">
            The main feature of the system is now live on the web: guards can create visitor entries, save them to Supabase through the backend, and process check-out actions in one place.
          </p>
        </div>

        <div className="profile-card">
          <span className="eyebrow">Session</span>
          <strong>{welcomeName}</strong>
          <span>{user.email}</span>
          <span className="role-pill">{user.role}</span>
          <button className="danger-button" type="button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>

      <main className="dashboard-main">
        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Main Feature</span>
              <h2>New Visitor Entry</h2>
            </div>
            <p className="copy">
              This form captures the required check-in details from the SDD, validates them, uploads the visitor ID image, and saves the record through the backend API.
            </p>
          </div>

          <form className="stack" onSubmit={handleVisitorSubmit}>
            <div className="grid-two">
              <label className="field">
                <span>Full Name</span>
                <input
                  value={visitorForm.fullName}
                  onChange={(event) => handleVisitorChange('fullName', event.target.value)}
                  placeholder="Juan Dela Cruz"
                />
              </label>
              <label className="field">
                <span>Contact Number</span>
                <input
                  value={visitorForm.contactNumber}
                  onChange={(event) => handleVisitorChange('contactNumber', event.target.value)}
                  placeholder="09171234567"
                />
              </label>
              <label className="field">
                <span>Host Name</span>
                <input
                  value={visitorForm.hostName}
                  onChange={(event) => handleVisitorChange('hostName', event.target.value)}
                  placeholder="Maria Santos"
                />
              </label>
              <label className="field">
                <span>Destination / Room</span>
                <input
                  value={visitorForm.destinationRoom}
                  onChange={(event) => handleVisitorChange('destinationRoom', event.target.value)}
                  placeholder="Room 204"
                />
              </label>
              <label className="field">
                <span>Visitor Type</span>
                <select
                  value={visitorForm.visitorType}
                  onChange={(event) => handleVisitorChange('visitorType', event.target.value)}
                >
                  <option value="Guest">Guest</option>
                  <option value="Employee">Employee</option>
                  <option value="Contractor">Contractor</option>
                  <option value="Courier">Courier</option>
                </select>
              </label>
              <label className="field">
                <span>ID Image</span>
                <input
                  type="file"
                  accept="image/*"
                  onChange={(event) => handleVisitorChange('idImage', event.target.files?.[0] ?? null)}
                />
              </label>
            </div>

            <label className="field">
              <span>Purpose of Visit</span>
              <textarea
                rows={4}
                value={visitorForm.purpose}
                onChange={(event) => handleVisitorChange('purpose', event.target.value)}
                placeholder="State the reason for the visit"
              />
            </label>

            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={visitorForm.extendedVisit}
                onChange={(event) => handleVisitorChange('extendedVisit', event.target.checked)}
              />
              <span>Extended Visit</span>
            </label>

            {dashboardFeedback.text && <div className={`feedback ${dashboardFeedback.type}`}>{dashboardFeedback.text}</div>}

            <button className="primary-button" type="submit" disabled={isVisitorSubmitting}>
              {isVisitorSubmitting ? 'Saving Visitor Entry...' : 'Submit Check-In'}
            </button>
          </form>
        </section>

        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Live Data</span>
              <h2>Active Visitor Logs</h2>
            </div>
            <p className="copy">
              This table reads directly from the backend and shows only visitor records whose status is currently active.
            </p>
          </div>

          {isLogsLoading ? (
            <p className="empty-state">Loading active visitor logs...</p>
          ) : activeLogs.length === 0 ? (
            <p className="empty-state">No active logs yet. Submit a visitor check-in to populate this dashboard.</p>
          ) : (
            <div className="table-wrap">
              <table className="logs-table">
                <thead>
                  <tr>
                    <th>Visitor</th>
                    <th>Type</th>
                    <th>Host</th>
                    <th>Destination</th>
                    <th>Time In</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {activeLogs.map((log) => (
                    <tr key={log.id}>
                      <td>{log.fullName}</td>
                      <td>{log.visitorType}</td>
                      <td>{log.hostName}</td>
                      <td>{log.destinationRoom}</td>
                      <td>{new Date(log.timeIn).toLocaleString()}</td>
                      <td>
                        <button className="table-button" type="button" onClick={() => handleCheckOut(log.id)}>
                          Check-Out
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default App
