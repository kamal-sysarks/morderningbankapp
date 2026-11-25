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
  Pagination,
  Tag,
  SkeletonText,
} from '@carbon/react'
import { getTransactionHistory, type Transaction } from '../api/banking'

const TYPE_COLORS: Record<string, 'green' | 'red' | 'orange' | 'blue' | 'gray'> = {
  DEPOSIT: 'green',
  WITHDRAWAL: 'red',
  TRANSFER_DEBIT: 'orange',
  TRANSFER_CREDIT: 'blue',
}

export default function TransactionHistoryPage() {
  const { accountNumber } = useParams<{ accountNumber: string }>()
  const navigate = useNavigate()
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!accountNumber) return
    setLoading(true)
    getTransactionHistory(accountNumber, page - 1, pageSize)
      .then((data) => {
        setTransactions(data.content)
        setTotalItems(data.totalElements)
      })
      .finally(() => setLoading(false))
  }, [accountNumber, page, pageSize])

  const headers = [
    { key: 'transactionDate', header: 'Date' },
    { key: 'transactionType', header: 'Type' },
    { key: 'amount', header: 'Amount' },
    { key: 'description', header: 'Description' },
    { key: 'referenceNumber', header: 'Reference' },
  ]

  const rows = transactions.map((t) => ({
    id: String(t.transactionId),
    transactionDate: t.transactionDate?.replace('T', ' ').substring(0, 16) ?? '',
    transactionType: t.transactionType,
    amount: `$${Number(t.amount).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`,
    description: t.description,
    referenceNumber: t.referenceNumber,
  }))

  return (
    <div style={{ background: '#161616', minHeight: '100vh', padding: '2rem' }}>
      <Button kind="ghost" size="sm" onClick={() => navigate(-1)} style={{ marginBottom: '1.5rem' }}>← Back</Button>
      <h1 style={{ color: '#f4f4f4', marginBottom: '0.5rem' }}>Transaction History</h1>
      <p style={{ color: '#8d8d8d', marginBottom: '1.5rem' }}>Account: {accountNumber}</p>

      {loading ? (
        <SkeletonText paragraph lineCount={6} />
      ) : rows.length === 0 ? (
        <p style={{ color: '#8d8d8d' }}>No transactions found.</p>
      ) : (
        <>
          <DataTable rows={rows} headers={headers}>
            {({ rows: tableRows, headers: tableHeaders, getTableProps, getHeaderProps, getRowProps }: any) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {tableHeaders.map((h: any) => (
                      <TableHeader {...getHeaderProps({ header: h })} key={h.key}>{h.header}</TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableRows.map((row: any) => (
                    <TableRow {...getRowProps({ row })} key={row.id}>
                      {row.cells.map((cell: any) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === 'transactionType' ? (
                            <Tag type={TYPE_COLORS[cell.value] ?? 'gray'} size="sm">{cell.value}</Tag>
                          ) : cell.value}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
          <Pagination
            totalItems={totalItems}
            pageSize={pageSize}
            pageSizes={[10, 20, 50]}
            page={page}
            onChange={({ page: p, pageSize: ps }) => { setPage(p); setPageSize(ps) }}
            style={{ marginTop: '1rem' }}
          />
        </>
      )}
    </div>
  )
}
