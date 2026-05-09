import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import {
  checkOutVisitor,
  createVisitorLog,
  fetchActiveLogs,
  loginUser,
  registerUser,
  fetchHistoricalLogs,
  voidVisitorLog,
  fetchLocations,
  addLocation,
  fetchStaff,
  fetchAuditLogs,
} from './api'
import type { Location, AuditLog } from './api'
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
  const [historicalLogs, setHistoricalLogs] = useState<VisitorLog[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [holidayBanner, setHolidayBanner] = useState('Checking holiday status...')
  const [activeTab, setActiveTab] = useState<'active' | 'history' | 'staff' | 'locations' | 'audit'>('active')

  const [staffList, setStaffList] = useState<UserData[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
  const [isStaffModalOpen, setIsStaffModalOpen] = useState(false)
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false)


  const [isAuthSubmitting, setIsAuthSubmitting] = useState(false)
  const [isVisitorSubmitting, setIsVisitorSubmitting] = useState(false)
  const [isLogsLoading, setIsLogsLoading] = useState(false)
  
  // Modal States
  const [isCheckInModalOpen, setIsCheckInModalOpen] = useState(false)
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false)

  useEffect(() => {
    const year = new Date().getFullYear();
    fetch(`https://date.nager.at/api/v3/PublicHolidays/${year}/PH`)
      .then(res => res.json())
      .then(data => {
        if (data && Array.isArray(data)) {
          const today = new Date().toISOString().split('T')[0];
          const upcoming = data.find((h: any) => h.date >= today);
          if (upcoming) {
            setHolidayBanner(`Upcoming Holiday (Philippines): ${upcoming.name} on ${upcoming.date}`);
          } else {
            setHolidayBanner('No upcoming public holidays in PH for the rest of the year.');
          }
        }
      })
      .catch(() => setHolidayBanner('Normal Access Day (PH)'))
  }, [])

  useEffect(() => {
    const storedUser = localStorage.getItem(storageKey)
    if (storedUser) {
      setUser(JSON.parse(storedUser))
    }
  }, [])

  useEffect(() => {
    if (user) {
      if (activeTab === 'active') void loadActiveLogs()
      else if (activeTab === 'history') void loadHistoricalLogs()
      else if (activeTab === 'staff') void loadStaff()
      else if (activeTab === 'locations') void loadLocations()
      else if (activeTab === 'audit') void loadAuditLogs()
      
      // Load locations globally for the check-in modal dropdown
      if (locations.length === 0) void loadLocations()
    }
  }, [user, activeTab])

  async function loadStaff() {
    setIsLogsLoading(true)
    try {
      const data = await fetchStaff()
      setStaffList(data)
    } catch (e) { console.error(e) }
    finally { setIsLogsLoading(false) }
  }

  async function loadLocations() {
    setIsLogsLoading(true)
    try {
      const data = await fetchLocations()
      setLocations(data)
    } catch (e) { console.error(e) }
    finally { setIsLogsLoading(false) }
  }

  async function loadAuditLogs() {
    setIsLogsLoading(true)
    try {
      const data = await fetchAuditLogs()
      setAuditLogs(data)
    } catch (e) { console.error(e) }
    finally { setIsLogsLoading(false) }
  }

  async function loadHistoricalLogs() {
    setIsLogsLoading(true)
    try {
      const pageData = await fetchHistoricalLogs(0, 50)
      setHistoricalLogs(pageData.content)
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to load historical logs.',
      })
    } finally {
      setIsLogsLoading(false)
    }
  }

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
      setIsCheckInModalOpen(false) // Close modal on success
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

  const handleVoid = async (logId: number) => {
    if (!user?.email || user.role !== 'ADMIN') return
    if (!window.confirm("Are you sure you want to void this record?")) return;

    try {
      await voidVisitorLog(logId, user.email)
      setDashboardFeedback({ type: 'success', text: 'Record voided successfully.' })
      await loadHistoricalLogs()
    } catch (error) {
      setDashboardFeedback({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to void record.',
      })
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
    setIsLogoutModalOpen(false)
  }

  // --- Auth Screens ---
  if (!user) {
    return (
      <div className="auth-shell">
        <div className="auth-card">
          <div className="auth-header">
            <div className="auth-logo">logo</div>
            <h1>Vigilo</h1>
            <p>Sign in to your account to continue</p>
          </div>

          <div className="mode-switch">
            <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Login</button>
            <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button>
          </div>

          {authFeedback.text && <div className={`feedback-alert ${authFeedback.type}`}>{authFeedback.text}</div>}

          {mode === 'login' ? (
            <form onSubmit={handleLogin} className="auth-form">
              <label>
                <span>Email Address</span>
                <input type="email" value={authForm.email} onChange={(e) => handleAuthChange('email', e.target.value)} required />
              </label>
              <label>
                <div className="label-row">
                  <span>Password</span>
                  <a href="#" className="forgot-link">Forgot Password?</a>
                </div>
                <input type="password" value={authForm.password} onChange={(e) => handleAuthChange('password', e.target.value)} required />
              </label>
              <button type="submit" className="primary-btn full-width" disabled={isAuthSubmitting}>
                {isAuthSubmitting ? 'Signing In...' : 'Login'}
              </button>
              <div className="divider"><span>or continue with</span></div>
              <button type="button" className="secondary-btn full-width">Sign in with Google</button>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="auth-form">
               <div className="split-row">
                 <label><span>First Name</span><input value={authForm.firstName} onChange={(e) => handleAuthChange('firstName', e.target.value)} required /></label>
                 <label><span>Last Name</span><input value={authForm.lastName} onChange={(e) => handleAuthChange('lastName', e.target.value)} required /></label>
               </div>
               <label><span>Email Address</span><input type="email" value={authForm.email} onChange={(e) => handleAuthChange('email', e.target.value)} required /></label>
               <label><span>Password</span><input type="password" value={authForm.password} onChange={(e) => handleAuthChange('password', e.target.value)} required /></label>
               <label>
                 <span>Role</span>
                 <select value={authForm.role} onChange={(e) => handleAuthChange('role', e.target.value)}>
                   <option value="STAFF">Staff</option>
                   <option value="ADMIN">Admin</option>
                 </select>
               </label>
               <button type="submit" className="primary-btn full-width" disabled={isAuthSubmitting}>
                 {isAuthSubmitting ? 'Registering...' : 'Register'}
               </button>
            </form>
          )}
        </div>
      </div>
    )
  }

  // --- Dashboard Screens ---
  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-logo">logo</div>
          <h2>Vigilo</h2>
        </div>
        
        <nav className="sidebar-nav">
          <div className="nav-group">
            <span className="nav-title">Visitor Management</span>
            <button className={`nav-item ${activeTab === 'active' ? 'active' : ''}`} onClick={() => setActiveTab('active')}>Active Logs</button>
            <button className={`nav-item ${activeTab === 'history' ? 'active' : ''}`} onClick={() => setActiveTab('history')}>Historical Logs</button>
          </div>

          {user.role === 'ADMIN' && (
            <div className="nav-group">
              <span className="nav-title">System</span>
              <button className={`nav-item ${activeTab === 'staff' ? 'active' : ''}`} onClick={() => setActiveTab('staff')}>Staff Management</button>
              <button className={`nav-item ${activeTab === 'locations' ? 'active' : ''}`} onClick={() => setActiveTab('locations')}>Facility Locations</button>
              <button className={`nav-item ${activeTab === 'audit' ? 'active' : ''}`} onClick={() => setActiveTab('audit')}>System Audit Log</button>
            </div>
          )}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="main-content">
        
        {/* Top Header */}
        <header className="top-header">
          <div className="api-banner">
            <strong>{holidayBanner}</strong>
          </div>
          <div className="header-toolbar">
            <input type="text" className="search-bar" placeholder="Search visitors or staff..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
            <div className="user-profile">
              <span className="role-badge">{user.role === 'ADMIN' ? 'Admin' : 'Staff'} Badge</span>
              <div className="user-details">
                <span className="user-name">{user.firstName} {user.lastName}</span>
                <span className="user-title">{user.role}</span>
              </div>
              <button className="dropdown-toggle" onClick={() => setIsLogoutModalOpen(true)}>▼</button>
            </div>
          </div>
        </header>

        {/* Dashboard Body */}
        <main className="dashboard-body">
          <div className="page-header">
            <div>
              <h1>{activeTab === 'active' ? 'Active Logs' : activeTab === 'history' ? 'Historical Logs' : activeTab === 'staff' ? 'Staff Management' : activeTab === 'locations' ? 'Location Management' : 'System Audit Log'}</h1>
              <p>{activeTab === 'active' ? `Currently monitoring ${activeLogs.length} active visitors.` : activeTab === 'history' ? `Viewing past visitor records.` : activeTab === 'staff' ? 'Manage system accounts and roles.' : activeTab === 'locations' ? 'Manage valid rooms and areas.' : 'Timeline of system events.'}</p>
            </div>
            {activeTab === 'active' && <button className="primary-btn" onClick={() => setIsCheckInModalOpen(true)}>New Visitor Check-in</button>}
            {activeTab === 'staff' && <button className="primary-btn" onClick={() => setIsStaffModalOpen(true)}>Register New Staff</button>}
            {activeTab === 'locations' && <button className="primary-btn" onClick={() => setIsLocationModalOpen(true)}>Add Location</button>}
          </div>

          {dashboardFeedback.text && <div className={`feedback-alert ${dashboardFeedback.type}`}>{dashboardFeedback.text}</div>}

          <div className="table-container">
            <table className="data-table">
              {activeTab === 'staff' ? (
                <>
                  <thead><tr><th>Full Name</th><th>Email Address</th><th>Role</th></tr></thead>
                  <tbody>
                    {staffList.filter(s => (s.firstName + ' ' + s.lastName).toLowerCase().includes(searchQuery.toLowerCase())).map((s, i) => <tr key={i}><td>{s.firstName} {s.lastName}</td><td>{s.email}</td><td>{s.role}</td></tr>)}
                  </tbody>
                </>
              ) : activeTab === 'locations' ? (
                <>
                  <thead><tr><th>Area Name</th><th>Room Number</th><th>Floor Level</th></tr></thead>
                  <tbody>
                    {locations.map((l, i) => <tr key={i}><td>{l.areaName}</td><td>{l.roomNumber}</td><td>{l.floorLevel}</td></tr>)}
                  </tbody>
                </>
              ) : activeTab === 'audit' ? (
                <>
                  <thead><tr><th>Timestamp</th><th>User</th><th>Action Performed</th><th>Details</th></tr></thead>
                  <tbody>
                    {auditLogs.map((a, i) => <tr key={i}><td>{new Date(a.timestamp).toLocaleString()}</td><td>{a.userEmail}</td><td>{a.actionPerformed}</td><td>{a.details}</td></tr>)}
                  </tbody>
                </>
              ) : (
                <>
                  <thead>
                <tr>
                  <th>Visitor Name</th>
                  <th>Type</th>
                  <th>Host (Tenant)</th>
                  <th>Destination/Room</th>
                  {activeTab === 'active' ? <th>Time in</th> : <th>Status</th>}
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {isLogsLoading ? (
                  <tr><td colSpan={6} className="text-center">Loading...</td></tr>
                ) : (activeTab === 'active' ? activeLogs : historicalLogs).length === 0 ? (
                  <tr><td colSpan={6} className="text-center">No records found.</td></tr>
                ) : (
                  (activeTab === 'active' ? activeLogs : historicalLogs).filter(l => l.fullName.toLowerCase().includes(searchQuery.toLowerCase())).map((log) => (
                    <tr key={log.id}>
                      <td>
                        {log.fullName}
                        {log.extendedVisit && <span className="extended-tag">Extended</span>}
                      </td>
                      <td><span className="type-box">{log.visitorType}</span></td>
                      <td>{log.hostName}</td>
                      <td>{log.destinationRoom}</td>
                      {activeTab === 'active' ? (
                         <td>{new Date(log.timeIn).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</td>
                      ) : (
                         <td>{log.status}</td>
                      )}
                      <td>
                        {activeTab === 'active' && <button className="action-btn" onClick={() => handleCheckOut(log.id)}>Check out</button>}
                        {activeTab === 'history' && user.role === 'ADMIN' && log.status !== 'Voided' && <button className="action-btn" onClick={() => handleVoid(log.id)}>Void</button>}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
              </>)}
            </table>
            <div className="table-footer">
              Showing {(activeTab === 'active' ? activeLogs : activeTab === 'history' ? historicalLogs : activeTab === 'staff' ? staffList : activeTab === 'locations' ? locations : auditLogs).length} records
            </div>
          </div>
        </main>
      </div>

      {/* --- Check-In Modal --- */}
      {isCheckInModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content check-in-modal">
            <h2>New Visitor Entry</h2>
            <form onSubmit={handleVisitorSubmit} className="modal-form">
              <div className="split-row">
                <label><span>Full Name</span><input value={visitorForm.fullName} onChange={(e) => handleVisitorChange('fullName', e.target.value)} placeholder="John Doe" required/></label>
                <label><span>Contact Number</span><input value={visitorForm.contactNumber} onChange={(e) => handleVisitorChange('contactNumber', e.target.value)} placeholder="09171234567" required/></label>
              </div>
              
              <div className="form-group-box">
                <div className="split-row">
                  <label><span>Host Name (Tenant)</span><input value={visitorForm.hostName} onChange={(e) => handleVisitorChange('hostName', e.target.value)} placeholder="Jane Doe" required/></label>
                  <label><span>Destination/Room</span>
                    <select value={visitorForm.destinationRoom} onChange={(e) => handleVisitorChange('destinationRoom', e.target.value)} required>
                      <option value="" disabled>Select Location</option>
                      {locations.map((loc, i) => (
                        <option key={i} value={`${loc.roomNumber} - ${loc.areaName}`}>{loc.roomNumber} - {loc.areaName}</option>
                      ))}
                    </select>
                  </label>
                </div>
              </div>

              <div className="split-row align-center">
                <label>
                  <span>Visitor Type</span>
                  <select value={visitorForm.visitorType} onChange={(e) => handleVisitorChange('visitorType', e.target.value)}>
                    <option value="Guest">Guest</option>
                    <option value="Employee">Employee</option>
                    <option value="Contractor">Contractor</option>
                    <option value="Courier">Courier</option>
                  </select>
                </label>
                <div className="toggle-container">
                  <label><span>Mark as Extended Visit</span>
                    <div className="toggle-switch">
                      <input type="checkbox" checked={visitorForm.extendedVisit} onChange={(e) => handleVisitorChange('extendedVisit', e.target.checked)}/>
                      <span className="slider"></span>
                    </div>
                  </label>
                  <small>Exempts this visitor from the automated 11:59 PM system checkout.</small>
                </div>
              </div>

              <label>
                <span>Purpose of Visit</span>
                <textarea rows={3} value={visitorForm.purpose} onChange={(e) => handleVisitorChange('purpose', e.target.value)} required></textarea>
              </label>

              <label>
                <span>Visitor ID Attachment</span>
                <input type="file" accept="image/*" className="file-input" onChange={(e) => handleVisitorChange('idImage', e.target.files?.[0] ?? null)} required/>
              </label>

              <div className="modal-actions">
                <button type="button" className="secondary-btn" onClick={() => setIsCheckInModalOpen(false)}>Cancel</button>
                <button type="submit" className="primary-btn" disabled={isVisitorSubmitting}>
                  {isVisitorSubmitting ? 'Submitting...' : 'Submit Check-in'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- Logout Modal --- */}
      {isLogoutModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content logout-modal">
            <div className="logout-icon">icon</div>
            <h2>Ready to Leave?</h2>
            <p>Are you sure you want to log out of your session?<br/>You will need to sign in again to access the dashboard.</p>
            <div className="modal-actions-center">
              <button className="secondary-btn" onClick={() => setIsLogoutModalOpen(false)}>Cancel</button>
              <button className="primary-btn" onClick={handleLogout}>Logout</button>
            </div>
          </div>
        </div>
      )}

      {/* Staff Modal */}
      {isStaffModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Register New Staff</h2>
            <form onSubmit={async (e) => {
              e.preventDefault()
              const target = e.target as typeof e.target & {
                firstName: { value: string }; lastName: { value: string }; email: { value: string }; password: { value: string }; role: { value: string }
              }
              await registerUser({
                firstName: target.firstName.value, lastName: target.lastName.value,
                email: target.email.value, password: target.password.value, role: target.role.value
              })
              setIsStaffModalOpen(false)
              loadStaff()
            }}>
              <div className="form-group"><label>First Name</label><input name="firstName" required /></div>
              <div className="form-group"><label>Last Name</label><input name="lastName" required /></div>
              <div className="form-group"><label>Email</label><input name="email" required type="email" /></div>
              <div className="form-group"><label>Password</label><input name="password" required type="password" /></div>
              <div className="form-group"><label>Role</label><select name="role"><option value="STAFF">STAFF</option><option value="ADMIN">ADMIN</option></select></div>
              <div className="modal-actions">
                <button type="button" className="secondary-btn" onClick={() => setIsStaffModalOpen(false)}>Cancel</button>
                <button type="submit" className="primary-btn">Register User</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Location Modal */}
      {isLocationModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Add Location</h2>
            <form onSubmit={async (e) => {
              e.preventDefault()
              const target = e.target as typeof e.target & {
                areaName: { value: string }; roomNumber: { value: string }; floorLevel: { value: string }
              }
              await addLocation({
                areaName: target.areaName.value, roomNumber: target.roomNumber.value, floorLevel: target.floorLevel.value
              })
              setIsLocationModalOpen(false)
              loadLocations()
            }}>
              <div className="form-group"><label>Area Name</label><input name="areaName" required /></div>
              <div className="form-group"><label>Room Number</label><input name="roomNumber" required /></div>
              <div className="form-group"><label>Floor Level</label><input name="floorLevel" required /></div>
              <div className="modal-actions">
                <button type="button" className="secondary-btn" onClick={() => setIsLocationModalOpen(false)}>Cancel</button>
                <button type="submit" className="primary-btn">Add Location</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  )
}

export default App