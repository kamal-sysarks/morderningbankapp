import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
  Select,
  SelectItem,
  TextInput,
  Tile,
} from '@carbon/react'
import { getCustomerById, getCustomerByAccount, type CustomerWithAccounts } from '../api/banking'

export default function CustomerSearchPage() {
  const [searchType, setSearchType] = useState<'customerId' | 'accountNumber'>('customerId')
  const [query, setQuery] = useState('')
  const [result, setResult] = useState<CustomerWithAccounts | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSearch = async () => {
    if (!query.trim()) {
      setError('Please enter a search value.')
      return
    }
    if (searchType === 'customerId' && isNaN(Number(query))) {
      setError('Customer ID must be a number.')
      return
    }
    setError('')
    setLoading(true)
    setResult(null)
    try {
      const data =
        searchType === 'customerId'
          ? await getCustomerById(Number(query))
          : await getCustomerByAccount(query.trim())
      setResult(data)
    } catch (err: any) {
      setError(err?.response?.data?.message || 'No customer found.')
    } finally {
      setLoading(false)
    }
  }

  const headers = [
    { key: 'accountNumber', header: 'Account Number' },
    { key: 'accountType', header: 'Type' },
    { key: 'status', header: 'Status' },
    { key: 'balance', header: 'Balance' },
  ]

  const rows =
    result?.accounts.map((a) => ({
      id: a.accountNumber,
      accountNumber: a.accountNumber,
      accountType: a.accountType,
      status: a.status,
      balance: `$${Number(a.balance).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`,
    })) ?? []

  return (
    <div style={{ background: '#161616', minHeight: '100vh', padding: '2rem' }}>
      <Button kind="ghost" size="sm" onClick={() => navigate('/dashboard')} style={{ marginBottom: '1.5rem' }}>
        ← Back to Dashboard
      </Button>
      <h1 style={{ color: '#f4f4f4', marginBottom: '1.5rem' }}>Customer Search</h1>

      <Tile style={{ marginBottom: '1.5rem', padding: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <Select
            id="searchType"
            labelText="Search by"
            value={searchType}
            onChange={(e) => setSearchType(e.target.value as 'customerId' | 'accountNumber')}
            style={{ width: 200 }}
          >
            <SelectItem value="customerId" text="Customer ID" />
            <SelectItem value="accountNumber" text="Account Number" />
          </Select>
          <TextInput
            id="query"
            labelText="Search value"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            style={{ width: 220 }}
          />
          <Button onClick={handleSearch} disabled={loading}>
            {loading ? 'Searching…' : 'Search'}
          </Button>
        </div>
      </Tile>

      {error && (
        <InlineNotification
          kind="error"
          title="Not found"
          subtitle={error}
          hideCloseButton
          style={{ marginBottom: '1rem' }}
        />
      )}

      {result && (
        <>
          <Tile style={{ marginBottom: '1.5rem', padding: '1.5rem' }}>
            <h3 style={{ color: '#f4f4f4', marginBottom: '1rem' }}>Customer Details</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '150px 1fr', gap: '0.5rem 1rem', color: '#c6c6c6' }}>
              <span style={{ color: '#8d8d8d' }}>Customer ID</span><span>{result.customer.customerId}</span>
              <span style={{ color: '#8d8d8d' }}>Name</span><span>{result.customer.customerName}</span>
              <span style={{ color: '#8d8d8d' }}>Email</span><span>{result.customer.email}</span>
              <span style={{ color: '#8d8d8d' }}>Phone</span><span>{result.customer.phone}</span>
              <span style={{ color: '#8d8d8d' }}>Address</span><span>{result.customer.address}</span>
            </div>
          </Tile>

          <h3 style={{ color: '#f4f4f4', marginBottom: '1rem' }}>Accounts</h3>
          <DataTable rows={rows} headers={headers}>
            {({ rows: tableRows, headers: tableHeaders, getTableProps, getHeaderProps, getRowProps }: any) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {tableHeaders.map((h: any) => (
                      <TableHeader {...getHeaderProps({ header: h })} key={h.key}>
                        {h.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableRows.map((row: any) => (
                    <TableRow
                      {...getRowProps({ row })}
                      key={row.id}
                      onClick={() => navigate(`/accounts/${row.id}`)}
                      style={{ cursor: 'pointer' }}
                    >
                      {row.cells.map((cell: any) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        </>
      )}
    </div>
  )
}
