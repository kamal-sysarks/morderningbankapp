import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  Tag,
  Tile,
  SkeletonText,
} from '@carbon/react'
import { getAccountDetails, getTransactionHistory, type Account, type Transaction } from '../api/banking'

const STATUS_COLORS: Record<string, 'green' | 'yellow' | 'red' | 'gray'> = {
  ACTIVE: 'green',
  FROZEN: 'yellow',
  CLOSED: 'red',
}

export default function AccountDetailsPage() {
  const { accountNumber } = useParams<{ accountNumber: string }>()
  const navigate = useNavigate()
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!accountNumber) return
    Promise.all([
      getAccountDetails(accountNumber),
      getTransactionHistory(accountNumber, 0, 10),
    ])
      .then(([acc, txns]) => {
        setAccount(acc)
        setTransactions(txns.content)
      })
      .catch((err) => setError(err?.response?.data?.message || 'Failed to load account details.'))
      .finally(() => setLoading(false))
  }, [accountNumber])

  if (loading) return <div style={{ padding: '2rem', color: '#f4f4f4' }}><SkeletonText paragraph /></div>
  if (error) return (
    <div style={{ padding: '2rem' }}>
      <InlineNotification kind="error" title="Error" subtitle={error} hideCloseButton />
    </div>
  )

  const txnHeaders = [
    { key: 'transactionDate', header: 'Date' },
    { key: 'transactionType', header: 'Type' },
    { key: 'amount', header: 'Amount' },
    { key: 'description', header: 'Description' },
    { key: 'referenceNumber', header: 'Reference' },
  ]

  const txnRows = transactions.map((t) => ({
    id: String(t.transactionId),
    transactionDate: t.transactionDate?.replace('T', ' ').substring(0, 16) ?? '',
    transactionType: t.transactionType,
    amount: `$${Number(t.amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`,
    description: t.description,
    referenceNumber: t.referenceNumber,
  }))

  return (
    <div style={{ background: '#161616', minHeight: '100vh', padding: '2rem' }}>
      <Button kind="ghost" size="sm" onClick={() => navigate(-1)} style={{ marginBottom: '1.5rem' }}>
        ← Back
      </Button>
      <h1 style={{ color: '#f4f4f4', marginBottom: '1.5rem' }}>Account Details</h1>

      <Tile style={{ marginBottom: '1.5rem', padding: '1.5rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '160px 1fr', gap: '0.75rem 1rem', color: '#c6c6c6', marginBottom: '1.5rem' }}>
          <span style={{ color: '#8d8d8d' }}>Account Number</span><span>{account?.accountNumber}</span>
          <span style={{ color: '#8d8d8d' }}>Account Type</span><span>{account?.accountType}</span>
          <span style={{ color: '#8d8d8d' }}>Status</span>
          <span>
            <Tag type={STATUS_COLORS[account?.status ?? ''] ?? 'gray'}>{account?.status}</Tag>
          </span>
          <span style={{ color: '#8d8d8d' }}>Balance</span>
          <span style={{ fontSize: '1.25rem', fontWeight: 600, color: '#42be65' }}>
            ${Number(account?.balance ?? 0).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
          </span>
        </div>

        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Button size="sm" kind="secondary" onClick={() => navigate(`/transactions/deposit?account=${accountNumber}`)}>
            Deposit
          </Button>
          <Button size="sm" kind="secondary" onClick={() => navigate(`/transactions/withdraw?account=${accountNumber}`)}>
            Withdraw
          </Button>
          <Button size="sm" kind="secondary" onClick={() => navigate(`/transactions/transfer?from=${accountNumber}`)}>
            Transfer
          </Button>
          <Button size="sm" kind="ghost" onClick={() => navigate(`/accounts/${accountNumber}/transactions`)}>
            Full History
          </Button>
        </div>
      </Tile>

      <h3 style={{ color: '#f4f4f4', marginBottom: '1rem' }}>Recent Transactions</h3>
      {txnRows.length === 0 ? (
        <p style={{ color: '#8d8d8d' }}>No transactions yet.</p>
      ) : (
        <DataTable rows={txnRows} headers={txnHeaders}>
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }: any) => (
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {headers.map((h: any) => (
                    <TableHeader {...getHeaderProps({ header: h })} key={h.key}>{h.header}</TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row: any) => (
                  <TableRow {...getRowProps({ row })} key={row.id}>
                    {row.cells.map((cell: any) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DataTable>
      )}
    </div>
  )
}
