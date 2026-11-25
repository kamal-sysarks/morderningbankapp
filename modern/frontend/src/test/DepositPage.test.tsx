import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import DepositPage from '../pages/DepositPage'
import { AuthProvider } from '../context/AuthContext'

vi.mock('../api/banking', () => ({
  deposit: vi.fn(),
}))

import { deposit } from '../api/banking'

const renderDeposit = () =>
  render(
    <MemoryRouter initialEntries={['/transactions/deposit']}>
      <AuthProvider>
        <DepositPage />
      </AuthProvider>
    </MemoryRouter>
  )

describe('DepositPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders deposit form fields', () => {
    renderDeposit()
    expect(screen.getByLabelText(/account number/i)).toBeDefined()
    expect(screen.getByLabelText(/amount/i)).toBeDefined()
    expect(screen.getByRole('button', { name: /deposit/i })).toBeDefined()
  })

  it('shows validation error for empty account', async () => {
    renderDeposit()
    fireEvent.click(screen.getByRole('button', { name: /deposit/i }))
    await waitFor(() => {
      expect(screen.getByText(/please enter the account number/i)).toBeDefined()
    })
  })

  it('shows validation error for zero amount', async () => {
    renderDeposit()
    await userEvent.type(screen.getByLabelText(/account number/i), '1001000001')
    await userEvent.type(screen.getByLabelText(/amount/i), '0')
    fireEvent.click(screen.getByRole('button', { name: /deposit/i }))
    await waitFor(() => {
      expect(screen.getByText(/must be greater than zero/i)).toBeDefined()
    })
  })

  it('shows success notification after deposit', async () => {
    const mockDeposit = deposit as ReturnType<typeof vi.fn>
    mockDeposit.mockResolvedValue({ referenceNumber: 'DEP0000000001', newBalance: 1250.00, message: 'Deposit successful.' })

    renderDeposit()
    await userEvent.type(screen.getByLabelText(/account number/i), '1001000001')
    await userEvent.type(screen.getByLabelText(/amount/i), '250.00')
    fireEvent.click(screen.getByRole('button', { name: /deposit/i }))

    await waitFor(() => {
      expect(screen.getByText(/deposit successful/i)).toBeDefined()
      expect(screen.getByText(/DEP0000000001/i)).toBeDefined()
    })
  })

  it('shows error notification on API failure', async () => {
    const mockDeposit = deposit as ReturnType<typeof vi.fn>
    mockDeposit.mockRejectedValue({ response: { data: { message: 'Account 9999 does not exist.' } } })

    renderDeposit()
    await userEvent.type(screen.getByLabelText(/account number/i), '9999')
    await userEvent.type(screen.getByLabelText(/amount/i), '100')
    fireEvent.click(screen.getByRole('button', { name: /deposit/i }))

    await waitFor(() => {
      expect(screen.getByText(/account 9999 does not exist/i)).toBeDefined()
    })
  })
})
