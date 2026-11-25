import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button,
  Grid,
  Column,
  Tile,
  SkeletonText,
  InlineNotification,
} from '@carbon/react'
import {
  UserMultiple,
  Account,
  Currency,
  Add,
  Subtract,
  ArrowsHorizontal,
  Search,
} from '@carbon/icons-react'
import { useAuth } from '../context/AuthContext'
import { getDashboardSummary, type DashboardSummary } from '../api/banking'

export default function DashboardPage() {
  const { employeeName, logout } = useAuth()
  const navigate = useNavigate()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getDashboardSummary()
      .then(setSummary)
      .catch(() => setError('Could not load dashboard data.'))
      .finally(() => setLoading(false))
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div style={{ background: '#161616', minHeight: '100vh' }}>
      {/* Header */}
      <div
        style={{
          background: '#262626',
          padding: '1rem 2rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '1px solid #393939',
        }}
      >
        <span style={{ color: '#f4f4f4', fontSize: '1rem', fontWeight: 600 }}>
          Legacy Bank — Branch Portal
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <span style={{ color: '#8d8d8d', fontSize: '0.875rem' }}>
            {employeeName}
          </span>
          <Button kind="ghost" size="sm" onClick={handleLogout}>
            Sign Out
          </Button>
        </div>
      </div>

      <div style={{ padding: '2rem' }}>
        <h1 style={{ color: '#f4f4f4', marginBottom: '0.5rem' }}>Dashboard</h1>
        <p style={{ color: '#8d8d8d', marginBottom: '2rem' }}>
          Welcome back, {employeeName}
        </p>

        {error && (
          <InlineNotification
            kind="error"
            title="Error"
            subtitle={error}
            hideCloseButton
            style={{ marginBottom: '1.5rem' }}
          />
        )}

        {/* KPI Tiles */}
        <Grid style={{ marginBottom: '2rem' }}>
          <Column sm={4} md={4} lg={4}>
            <Tile style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <p style={{ color: '#8d8d8d', fontSize: '0.75rem', marginBottom: '0.5rem' }}>
                    TOTAL CUSTOMERS
                  </p>
                  {loading ? (
                    <SkeletonText width="60px" />
                  ) : (
                    <p style={{ color: '#f4f4f4', fontSize: '2rem', fontWeight: 600 }}>
                      {summary?.totalCustomers ?? '—'}
                    </p>
                  )}
                </div>
                <UserMultiple size={32} style={{ color: '#4589ff' }} />
              </div>
            </Tile>
          </Column>
          <Column sm={4} md={4} lg={4}>
            <Tile style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <p style={{ color: '#8d8d8d', fontSize: '0.75rem', marginBottom: '0.5rem' }}>
                    ACTIVE ACCOUNTS
                  </p>
                  {loading ? (
                    <SkeletonText width="60px" />
                  ) : (
                    <p style={{ color: '#f4f4f4', fontSize: '2rem', fontWeight: 600 }}>
                      {summary?.activeAccounts ?? '—'}
                    </p>
                  )}
                </div>
                <Account size={32} style={{ color: '#42be65' }} />
              </div>
            </Tile>
          </Column>
          <Column sm={4} md={4} lg={4}>
            <Tile style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <p style={{ color: '#8d8d8d', fontSize: '0.75rem', marginBottom: '0.5rem' }}>
                    TODAY'S TRANSACTIONS
                  </p>
                  {loading ? (
                    <SkeletonText width="60px" />
                  ) : (
                    <p style={{ color: '#f4f4f4', fontSize: '2rem', fontWeight: 600 }}>
                      {summary?.todayTransactions ?? '—'}
                    </p>
                  )}
                </div>
                <Currency size={32} style={{ color: '#f1c21b' }} />
              </div>
            </Tile>
          </Column>
        </Grid>

        {/* Quick Actions */}
        <h2 style={{ color: '#f4f4f4', marginBottom: '1rem', fontSize: '1rem' }}>Quick Actions</h2>
        <Grid>
          <Column sm={2} md={2} lg={3}>
            <Button
              kind="secondary"
              renderIcon={Search}
              style={{ width: '100%', marginBottom: '1rem' }}
              onClick={() => navigate('/customers/search')}
            >
              Search Customer
            </Button>
          </Column>
          <Column sm={2} md={2} lg={3}>
            <Button
              kind="secondary"
              renderIcon={Add}
              style={{ width: '100%', marginBottom: '1rem' }}
              onClick={() => navigate('/transactions/deposit')}
            >
              Deposit
            </Button>
          </Column>
          <Column sm={2} md={2} lg={3}>
            <Button
              kind="secondary"
              renderIcon={Subtract}
              style={{ width: '100%', marginBottom: '1rem' }}
              onClick={() => navigate('/transactions/withdraw')}
            >
              Withdraw
            </Button>
          </Column>
          <Column sm={2} md={2} lg={3}>
            <Button
              kind="secondary"
              renderIcon={ArrowsHorizontal}
              style={{ width: '100%', marginBottom: '1rem' }}
              onClick={() => navigate('/transactions/transfer')}
            >
              Transfer
            </Button>
          </Column>
        </Grid>
      </div>
    </div>
  )
}
