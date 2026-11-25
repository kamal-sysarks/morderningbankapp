import React, { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Form, InlineNotification, TextInput, Tile } from '@carbon/react'
import { transfer } from '../api/banking'

export default function TransferPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [fromAccount, setFromAccount] = useState(searchParams.get('from') ?? '')
  const [toAccount, setToAccount] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [success, setSuccess] = useState<{ reference: string; fromBalance: number; toBalance: number } | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const validate = (): string => {
    if (!fromAccount.trim() || !/^[A-Za-z0-9]{1,20}$/.test(fromAccount.trim())) return 'Please enter a valid source account number.'
    if (!toAccount.trim() || !/^[A-Za-z0-9]{1,20}$/.test(toAccount.trim())) return 'Please enter a valid destination account number.'
    if (fromAccount.trim() === toAccount.trim()) return 'Source and destination accounts must be different.'
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
      const res = await transfer(fromAccount.trim(), toAccount.trim(), parseFloat(amount), description || undefined)
      setSuccess({ reference: res.referenceNumber, fromBalance: res.fromBalance, toBalance: res.toBalance })
      setAmount('')
      setDescription('')
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Transfer failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const fmt = (n: number) => `$${n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`

  return (
    <div style={{ background: '#161616', minHeight: '100vh', padding: '2rem' }}>
      <Button kind="ghost" size="sm" onClick={() => navigate(-1)} style={{ marginBottom: '1.5rem' }}>← Back</Button>
      <h1 style={{ color: '#f4f4f4', marginBottom: '1.5rem' }}>Fund Transfer</h1>
      <Tile style={{ maxWidth: 520, padding: '2rem' }}>
        {success && (
          <InlineNotification kind="success" title="Transfer successful"
            subtitle={`Reference: ${success.reference} | Source: ${fmt(success.fromBalance)} | Destination: ${fmt(success.toBalance)}`}
            hideCloseButton style={{ marginBottom: '1.5rem' }} />
        )}
        {error && (
          <InlineNotification kind="error" title="Transfer failed" subtitle={error} hideCloseButton style={{ marginBottom: '1.5rem' }} />
        )}
        <Form onSubmit={handleSubmit}>
          <TextInput id="trf-from" labelText="Source Account Number" value={fromAccount} onChange={(e) => setFromAccount(e.target.value)} style={{ marginBottom: '1rem' }} />
          <TextInput id="trf-to" labelText="Destination Account Number" value={toAccount} onChange={(e) => setToAccount(e.target.value)} style={{ marginBottom: '1rem' }} />
          <TextInput id="trf-amount" labelText="Amount" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" style={{ marginBottom: '1rem' }} />
          <TextInput id="trf-desc" labelText="Description (optional)" value={description} onChange={(e) => setDescription(e.target.value)} style={{ marginBottom: '1.5rem' }} />
          <Button type="submit" disabled={loading}>{loading ? 'Processing…' : 'Transfer'}</Button>
        </Form>
      </Tile>
    </div>
  )
}
