package com.modernbank.service;

import com.modernbank.dto.PagedTransactions;
import com.modernbank.dto.TransactionRequest;
import com.modernbank.dto.TransactionResponse;
import com.modernbank.dto.TransferRequest;
import com.modernbank.dto.TransferResponse;
import com.modernbank.exception.BankingException;
import com.modernbank.exception.ResourceNotFoundException;
import com.modernbank.model.Account;
import com.modernbank.model.Transaction;
import com.modernbank.repository.AccountRepository;
import com.modernbank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TransactionService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse deposit(TransactionRequest request, String performedBy) {
        String accountNumber = validateAccountNumber(request.getAccountNumber());
        BigDecimal amount = validateAmount(request.getAmount());

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " does not exist."));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BankingException("Account " + accountNumber + " is not active (status: " + account.getStatus() + ").");
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        String reference = nextReference("DEP");
        String description = buildDescription(request.getDescription(), "Cash deposit", performedBy);

        accountRepository.updateBalance(accountNumber, newBalance);
        transactionRepository.insert(accountNumber, "DEPOSIT", amount, description, reference);

        return new TransactionResponse(reference, newBalance, "Deposit successful.");
    }

    @Transactional
    public TransactionResponse withdraw(TransactionRequest request, String performedBy) {
        String accountNumber = validateAccountNumber(request.getAccountNumber());
        BigDecimal amount = validateAmount(request.getAmount());

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " does not exist."));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BankingException("Account " + accountNumber + " is not active (status: " + account.getStatus() + ").");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BankingException("Insufficient funds in account. Available balance is "
                    + account.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        String reference = nextReference("WDR");
        String description = buildDescription(request.getDescription(), "Cash withdrawal", performedBy);

        accountRepository.updateBalance(accountNumber, newBalance);
        transactionRepository.insert(accountNumber, "WITHDRAWAL", amount, description, reference);

        return new TransactionResponse(reference, newBalance, "Withdrawal successful.");
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String performedBy) {
        String from = validateAccountNumber(request.getFromAccount());
        String to = validateAccountNumber(request.getToAccount());
        BigDecimal amount = validateAmount(request.getAmount());

        if (from.equals(to)) {
            throw new BankingException("Source and destination accounts must be different.");
        }

        Account fromAccount = accountRepository.findByAccountNumber(from)
                .orElseThrow(() -> new ResourceNotFoundException("Source account " + from + " does not exist."));
        Account toAccount = accountRepository.findByAccountNumber(to)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account " + to + " does not exist."));

        if (!"ACTIVE".equals(fromAccount.getStatus())) {
            throw new BankingException("Source account " + from + " is not active (status: " + fromAccount.getStatus() + ").");
        }
        if (!"ACTIVE".equals(toAccount.getStatus())) {
            throw new BankingException("Destination account " + to + " is not active (status: " + toAccount.getStatus() + ").");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new BankingException("Insufficient funds in source account. Available balance is "
                    + fromAccount.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
        }

        BigDecimal newFrom = fromAccount.getBalance().subtract(amount);
        BigDecimal newTo = toAccount.getBalance().add(amount);
        String reference = nextReference("TRF");

        String userNote = (request.getDescription() != null && !request.getDescription().isBlank())
                ? " - " + request.getDescription().trim() : "";
        String suffix = userNote + " (by " + performedBy + ")";

        accountRepository.updateBalance(from, newFrom);
        accountRepository.updateBalance(to, newTo);
        transactionRepository.insert(from, "TRANSFER_DEBIT", amount, "Transfer to " + to + suffix, reference);
        transactionRepository.insert(to, "TRANSFER_CREDIT", amount, "Transfer from " + from + suffix, reference);

        return new TransferResponse(reference, newFrom, newTo, "Transfer successful.");
    }

    public PagedTransactions getHistory(String accountNumber, int page, int size) {
        validateAccountNumber(accountNumber);
        // Verify account exists
        accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " does not exist."));

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        int offset = safePage * safeSize;

        List<Transaction> rows = transactionRepository.findByAccountNumber(accountNumber, offset, safeSize);
        long total = transactionRepository.countByAccountNumber(accountNumber);
        return new PagedTransactions(rows, total, safePage, safeSize);
    }

    // -------------------------------------------------------------------------

    private String nextReference(String prefix) {
        long id = transactionRepository.nextRefId();
        String digits = String.valueOf(id);
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = digits.length(); i < 10; i++) sb.append('0');
        return sb.append(digits).toString();
    }

    static String validateAccountNumber(String text) {
        if (text == null || text.isBlank()) {
            throw new BankingException("Please enter the account number.");
        }
        String value = text.trim();
        if (!value.matches("[A-Za-z0-9]{1,20}")) {
            throw new BankingException("Account number may only contain letters and digits (max 20).");
        }
        return value;
    }

    static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BankingException("Please enter an amount.");
        }
        if (amount.signum() <= 0) {
            throw new BankingException("Amount must be greater than zero.");
        }
        if (amount.scale() > 2 && amount.stripTrailingZeros().scale() > 2) {
            throw new BankingException("Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BankingException("Amount cannot exceed 10,000,000.00 per transaction.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String buildDescription(String userInput, String defaultDesc, String performedBy) {
        String desc = (userInput != null && !userInput.isBlank()) ? userInput.trim() : defaultDesc;
        return desc + " (by " + performedBy + ")";
    }
}
