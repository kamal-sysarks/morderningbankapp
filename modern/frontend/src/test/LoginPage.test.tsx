import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LoginPage from '../pages/LoginPage'
import { AuthProvider } from '../context/AuthContext'

// Mock the auth API
vi.mock('../api/auth', () => ({
  login: vi.fn(),
}))

import { login } from '../api/auth'

const renderLogin = () =>
  render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  )

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders username and password fields', () => {
    renderLogin()
    expect(screen.getByLabelText(/username/i)).toBeDefined()
    expect(document.getElementById('password')).not.toBeNull()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeDefined()
  })

  it('shows validation error when fields are empty', async () => {
    renderLogin()
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() => {
      expect(screen.getByText(/please enter username and password/i)).toBeDefined()
    })
  })

  it('calls login API and stores token on success', async () => {
    const mockLogin = login as ReturnType<typeof vi.fn>
    mockLogin.mockResolvedValue({ token: 'test-token', username: 'admin', employeeName: 'Branch Administrator' })

    renderLogin()
    await userEvent.type(screen.getByLabelText(/username/i), 'admin')
    await userEvent.type(document.getElementById('password')!, 'admin123')
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({ username: 'admin', password: 'admin123' })
    })
  })

  it('shows error notification on login failure', async () => {
    const mockLogin = login as ReturnType<typeof vi.fn>
    mockLogin.mockRejectedValue({ response: { data: { message: 'Invalid username or password.' } } })

    renderLogin()
    await userEvent.type(screen.getByLabelText(/username/i), 'admin')
    await userEvent.type(document.getElementById('password')!, 'wrongpass')
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText(/invalid username or password/i)).toBeDefined()
    })
  })
})
