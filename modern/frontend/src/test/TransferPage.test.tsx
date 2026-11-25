import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import TransferPage from '../pages/TransferPage'
import { AuthProvider } from '../context/AuthContext'

vi.mock('../api/banking', () => ({
  transfer: vi.fn(),
}))

import { transfer } from '../api/banking'

const renderTransfer = () =>
  render(
    <MemoryRouter initialEntries={['/transactions/transfer']}>
      <AuthProvider>
        <TransferPage />
      </AuthProvider>
    </MemoryRouter>
  )

describe('TransferPage', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders transfer form fields', () => {
    renderTransfer()
    expect(screen.getByLabelText(/source account/i)).toBeDefined()
    expect(screen.getByLabelText(/destination account/i)).toBeDefined()
    expect(screen.getByLabelText(/amount/i)).toBeDefined()
  })

  it('shows error when source equals destination', async () => {
    renderTransfer()
    await userEvent.type(screen.getByLabelText(/source account/i), '1001000001')
    await userEvent.type(screen.getByLabelText(/destination account/i), '1001000001')
    await userEvent.type(screen.getByLabelText(/amount/i), '100')
    fireEvent.click(screen.getByRole('button', { name: /transfer/i }))
    await waitFor(() => {
      expect(screen.getByText(/source and destination accounts must be different/i)).toBeDefined()
    })
  })

  it('shows success notification after transfer', async () => {
    const mockTransfer = transfer as ReturnType<typeof vi.fn>
    mockTransfer.mockResolvedValue({ referenceNumber: 'TRF0000000001', fromBalance: 800, toBalance: 700, message: 'Transfer successful.' })

    renderTransfer()
    await userEvent.type(screen.getByLabelText(/source account/i), '1001000001')
    await userEvent.type(screen.getByLabelText(/destination account/i), '1001000002')
    await userEvent.type(screen.getByLabelText(/amount/i), '200')
    fireEvent.click(screen.getByRole('button', { name: /transfer/i }))

    await waitFor(() => {
      expect(screen.getByText(/transfer successful/i)).toBeDefined()
      expect(screen.getByText(/TRF0000000001/i)).toBeDefined()
    })
  })
})
