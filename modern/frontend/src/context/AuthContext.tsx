import React, { createContext, useContext, useState, useCallback } from 'react'

interface AuthState {
  token: string | null
  username: string | null
  employeeName: string | null
}

interface AuthContextValue extends AuthState {
  setAuth: (token: string, username: string, employeeName: string) => void
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(() => ({
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
    employeeName: localStorage.getItem('employeeName'),
  }))

  const setAuth = useCallback((token: string, username: string, employeeName: string) => {
    localStorage.setItem('token', token)
    localStorage.setItem('username', username)
    localStorage.setItem('employeeName', employeeName)
    setState({ token, username, employeeName })
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('employeeName')
    setState({ token: null, username: null, employeeName: null })
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, setAuth, logout, isAuthenticated: !!state.token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
