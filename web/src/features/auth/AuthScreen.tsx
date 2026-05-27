import { useState } from 'react'
import { loginUser } from './authApi'
import type { Feedback } from '../../core/types'

export default function AuthScreen({ onLogin }: { onLogin: (user: any) => void }) {
  const [form, setForm] = useState({ email: '', password: '' })
  const [feedback, setFeedback] = useState<Feedback>({ type: '', text: '' })
  const [loading, setLoading] = useState(false)

  const handleChange = (field: 'email' | 'password', value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setFeedback({ type: '', text: '' })
    setLoading(true)
    try {
      const response = await loginUser({ email: form.email, password: form.password })
      setFeedback({ type: 'success', text: response.message })
      onLogin(response.data)
    } catch (error: any) {
      setFeedback({ type: 'error', text: error.message })
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
        {feedback.text && <div className={`feedback-alert ${feedback.type}`}>{feedback.text}</div>}
        <form onSubmit={handleSubmit} className="auth-form">
          <label><span>Email Address</span><input type="email" value={form.email} onChange={(event) => handleChange('email', event.target.value)} required /></label>
          <label><span>Password</span><input type="password" value={form.password} onChange={(event) => handleChange('password', event.target.value)} required /></label>
          <button type="submit" className="primary-btn full-width" disabled={loading}>{loading ? 'Processing...' : 'Login'}</button>
          <div className="divider"><span>or continue with</span></div>
          <button type="button" className="secondary-btn full-width">Sign in with Google</button>
        </form>
      </div>
    </div>
  )
}
