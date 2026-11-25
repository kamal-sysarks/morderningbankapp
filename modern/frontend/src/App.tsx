import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import CustomerSearchPage from './pages/CustomerSearchPage'
import AccountDetailsPage from './pages/AccountDetailsPage'
import DepositPage from './pages/DepositPage'
import WithdrawPage from './pages/WithdrawPage'
import TransferPage from './pages/TransferPage'
import TransactionHistoryPage from './pages/TransactionHistoryPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function AppRoutes() {
  const { isAuthenticated } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
      <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      <Route path="/customers/search" element={<ProtectedRoute><CustomerSearchPage /></ProtectedRoute>} />
      <Route path="/accounts/:accountNumber" element={<ProtectedRoute><AccountDetailsPage /></ProtectedRoute>} />
      <Route path="/accounts/:accountNumber/transactions" element={<ProtectedRoute><TransactionHistoryPage /></ProtectedRoute>} />
      <Route path="/transactions/deposit" element={<ProtectedRoute><DepositPage /></ProtectedRoute>} />
      <Route path="/transactions/withdraw" element={<ProtectedRoute><WithdrawPage /></ProtectedRoute>} />
      <Route path="/transactions/transfer" element={<ProtectedRoute><TransferPage /></ProtectedRoute>} />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
