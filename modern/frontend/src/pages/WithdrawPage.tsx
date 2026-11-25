import React, { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Form, InlineNotification, TextInput, Tile } from '@carbon/react'
import { withdraw } from '../api/banking'

export default function WithdrawPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [accountNumber, setAccountNumber] = useState(searchParams.get('account') ?? '')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [success, setSuccess] = useState<{ reference: string; balance: number } | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const validate = (): string => {
    if (!accountNumber.trim()) return 'Please enter the account number.'
    if (!/^[A-Za-z0-9]{1,20}$/.test(accountNumber.trim())) return 'Account number must be alphanumeric (max 20 chars).'
    const amt = parseFloat(amount)
    if (!amount || isNaN(amt)) return 'Please enter a valid amount.'
    if (amt <= 0) return 'Amount must be greater than zero.'
    if (!/^\d+(\.\d{1,2})?$/.test(amount)) return 'Amount cannot have more than two decimal places.'
    if (amt > 10000000) return 'Amount cannot exceed 10,000,000.00.'
    return ''
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const validationError = validate()
    if (validationError) { setError(validationError); return }
    setError('')
    setSuccess(null)
    setLoading(true)
    try {
      const res = await withdraw(accountNumber.trim(), parseFloat(amount), description || undefined)
      setSuccess({ reference: res.referenceNumber, balance: res.newBalance })
      setAmount('')
      setDescription('')
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Withdrawal failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ background: '#161616', minHeight: '100vh', padding: '2rem' }}>
      <Button kind="ghost" size="sm" onClick={() => navigate(-1)} style={{ marginBottom: '1.5rem' }}>← Back</Button>
      <h1 style={{ color: '#f4f4f4', marginBottom: '1.5rem' }}>Cash Withdrawal</h1>
      <Tile style={{ maxWidth: 480, padding: '2rem' }}>
        {success && (
          <InlineNotification kind="success" title="Withdrawal successful"
            subtitle={`Reference: ${success.reference} | New balance: $${Number(success.balance).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`}
            hideCloseButton style={{ marginBottom: '1.5rem' }} />
        )}
        {error && (
          <InlineNotification kind="error" title="Withdrawal failed" subtitle={error} hideCloseButton style={{ marginBottom: '1.5rem' }} />
        )}
        <Form onSubmit={handleSubmit}>
          <TextInput id="wdr-account" labelText="Account Number" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} style={{ marginBottom: '1rem' }} />
          <TextInput id="wdr-amount" labelText="Amount" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" style={{ marginBottom: '1rem' }} />
          <TextInput id="wdr-desc" labelText="Description (optional)" value={description} onChange={(e) => setDescription(e.target.value)} style={{ marginBottom: '1.5rem' }} />
          <Button type="submit" disabled={loading}>{loading ? 'Processing…' : 'Withdraw'}</Button>
        </Form>
      </Tile>
    </div>
  )
}
