import { useState } from 'react'
import { loginUser, registerUser } from './authApi'
import type { Feedback } from '../../core/types'

export default function AuthScreen({ onLogin }: { onLogin: (user: any) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', role: 'STAFF' })
  const [feedback, setFeedback] = useState<Feedback>({ type: '', text: '' })
  const [loading, setLoading] = useState(false)

  const handleChange = (f: string, v: string) => setForm(c => ({ ...c, [f]: v }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFeedback({ type: '', text: '' })
    setLoading(true)
    try {
      if (mode === 'login') {
        const res = await loginUser({ email: form.email, password: form.password })
        setFeedback({ type: 'success', text: res.message })
        onLogin(res.data)
      } else {
        const res = await registerUser(form)
        setFeedback({ type: 'success', text: res.message })
        setMode('login')
      }
    } catch (err: any) {
      setFeedback({ type: 'error', text: err.message })
    } finally {
      setLoading(false)
    }
  }

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
        {feedback.text && <div className={`feedback-alert ${feedback.type}`}>{feedback.text}</div>}
        <form onSubmit={handleSubmit} className="auth-form">
          {mode === 'register' && (
            <div className="split-row">
              <label><span>First Name</span><input value={form.firstName} onChange={e => handleChange('firstName', e.target.value)} required /></label>
              <label><span>Last Name</span><input value={form.lastName} onChange={e => handleChange('lastName', e.target.value)} required /></label>
            </div>
          )}
          <label><span>Email Address</span><input type="email" value={form.email} onChange={e => handleChange('email', e.target.value)} required /></label>
          <label><span>Password</span><input type="password" value={form.password} onChange={e => handleChange('password', e.target.value)} required /></label>
          {mode === 'register' && (
            <label><span>Role</span><select value={form.role} onChange={e => handleChange('role', e.target.value)}><option value="STAFF">Staff</option><option value="ADMIN">Admin</option></select></label>
          )}
          <button type="submit" className="primary-btn full-width" disabled={loading}>{loading ? 'Processing...' : (mode === 'login' ? 'Login' : 'Register')}</button>
          {mode === 'login' && <><div className="divider"><span>or continue with</span></div><button type="button" className="secondary-btn full-width">Sign in with Google</button></>}
        </form>
      </div>
    </div>
  )
}
