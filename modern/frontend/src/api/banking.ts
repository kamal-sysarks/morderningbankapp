import api from './client'

export interface Customer {
  customerId: number
  customerName: string
  email: string
  phone: string
  address: string
}

export interface Account {
  accountId: number
  accountNumber: string
  customerId: number
  accountType: string
  balance: number
  status: string
  createdDate?: string
}

export interface CustomerWithAccounts {
  customer: Customer
  accounts: Account[]
}

export interface DashboardSummary {
  totalCustomers: number
  activeAccounts: number
  todayTransactions: number
}

export interface Transaction {
  transactionId: number
  accountNumber: string
  transactionType: string
  amount: number
  description: string
  transactionDate: string
  referenceNumber: string
}

export interface PagedTransactions {
  content: Transaction[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface TransactionResponse {
  referenceNumber: string
  newBalance: number
  message: string
}

export interface TransferResponse {
  referenceNumber: string
  fromBalance: number
  toBalance: number
  message: string
}

export const getDashboardSummary = () =>
  api.get<DashboardSummary>('/api/dashboard/summary').then((r) => r.data)

export const getCustomerById = (id: number) =>
  api.get<CustomerWithAccounts>(`/api/customers/${id}`).then((r) => r.data)

export const getCustomerByAccount = (accountNumber: string) =>
  api.get<CustomerWithAccounts>(`/api/customers/by-account/${accountNumber}`).then((r) => r.data)

export const getAccountDetails = (accountNumber: string) =>
  api.get<Account>(`/api/accounts/${accountNumber}`).then((r) => r.data)

export const getTransactionHistory = (accountNumber: string, page = 0, size = 20) =>
  api.get<PagedTransactions>(`/api/accounts/${accountNumber}/transactions?page=${page}&size=${size}`).then((r) => r.data)

export const deposit = (accountNumber: string, amount: number, description?: string) =>
  api.post<TransactionResponse>('/api/transactions/deposit', { accountNumber, amount, description }).then((r) => r.data)

export const withdraw = (accountNumber: string, amount: number, description?: string) =>
  api.post<TransactionResponse>('/api/transactions/withdraw', { accountNumber, amount, description }).then((r) => r.data)

export const transfer = (fromAccount: string, toAccount: string, amount: number, description?: string) =>
  api.post<TransferResponse>('/api/transactions/transfer', { fromAccount, toAccount, amount, description }).then((r) => r.data)
