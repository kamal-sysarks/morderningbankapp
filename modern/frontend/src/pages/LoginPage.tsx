import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button,
  Form,
  InlineNotification,
  PasswordInput,
  TextInput,
  Tile,
} from '@carbon/react'
import { useAuth } from '../context/AuthContext'
import { login } from '../api/auth'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { setAuth } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!username.trim() || !password) {
      setError('Please enter username and password.')
      return
    }
    setLoading(true)
    setError('')
    try {
      const res = await login({ username: username.trim(), password })
      setAuth(res.token, res.username, res.employeeName)
      navigate('/dashboard')
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Login failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#161616',
      }}
    >
      <Tile style={{ width: 400, padding: '2.5rem' }}>
        <h2 style={{ marginBottom: '0.5rem', color: '#f4f4f4' }}>Legacy Bank</h2>
        <p style={{ color: '#8d8d8d', marginBottom: '2rem', fontSize: '0.875rem' }}>
          Branch Operations Portal
        </p>

        {error && (
          <InlineNotification
            kind="error"
            title="Login failed"
            subtitle={error}
            hideCloseButton
            style={{ marginBottom: '1.5rem' }}
          />
        )}

        <Form onSubmit={handleSubmit}>
          <TextInput
            id="username"
            labelText="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{ marginBottom: '1rem' }}
            autoFocus
          />
          <PasswordInput
            id="password"
            labelText="Password"
            value={password}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
            style={{ marginBottom: '1.5rem' }}
          />
          <Button type="submit" disabled={loading} style={{ width: '100%' }}>
            {loading ? 'Signing in…' : 'Sign In'}
          </Button>
        </Form>
      </Tile>
    </div>
  )
}
