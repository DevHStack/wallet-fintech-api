package com.hiral.wallet.account;

import com.hiral.wallet.account.exception.AccountNotFoundException;
import com.hiral.wallet.account.exception.BadRequestException;
import com.hiral.wallet.account.exception.InsufficientFundsException;
import com.hiral.wallet.account.exception.InvalidAmountException;
import com.hiral.wallet.account.transaction.AccountTransaction;
import com.hiral.wallet.account.transaction.AccountTransactionRepository;
import com.hiral.wallet.account.transaction.TransactionType;
import com.hiral.wallet.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final com.hiral.wallet.audit.AuditService auditService;

    public Account createAccount(String userId, String ownerName, String currency) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("Owner id is required");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new BadRequestException("Owner name is required");
        }
        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }

        Account account = Account.builder()
                .ownerId(userId.trim())
                .ownerName(ownerName.trim())
                .currency(currency.toUpperCase())
                .build();

        Account saved = accountRepository.save(account);
        recordTransaction(saved, TransactionType.DEPOSIT, BigDecimal.ZERO, "Account created");
        auditService.record(userId, saved.getId(), "CREATE_ACCOUNT", "Created account for " + ownerName);
        return saved;
    }

    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public Account deposit(String userId, Long accountId, BigDecimal amount, String currency) {
        validateAmount(amount);

        Account account = getAccount(accountId);
        if (!account.getOwnerId().equals(userId)) {
            throw new BadRequestException("Account does not belong to user");
        }
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new BadRequestException("Currency mismatch: expected " + account.getCurrency());
        }

        account.setBalance(account.getBalance().add(amount));
        recordTransaction(account, TransactionType.DEPOSIT, amount, "Deposit");
        auditService.record(userId, accountId, "DEPOSIT", "Amount " + amount + " " + currency);
        return account;
    }

    @Transactional
    public Account withdraw(String userId, Long accountId, BigDecimal amount, String currency) {
        validateAmount(amount);

        Account account = getAccount(accountId);
        if (!account.getOwnerId().equals(userId)) {
            throw new BadRequestException("Account does not belong to user");
        }
        if (!account.getCurrency().equalsIgnoreCase(currency)) {
            throw new BadRequestException("Currency mismatch: expected " + account.getCurrency());
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId, account.getCurrency());
        }

        account.setBalance(account.getBalance().subtract(amount));
        recordTransaction(account, TransactionType.WITHDRAWAL, amount.negate(), "Withdrawal");
        auditService.record(userId, accountId, "WITHDRAW", "Amount " + amount + " " + currency);
        return account;
    }

    @Transactional
    public void transfer(String userId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (Objects.equals(fromAccountId, toAccountId)) {
            throw new BadRequestException("Source and destination account must differ");
        }
        validateAmount(amount);

        // Load in stable order to reduce deadlock risk
        Long first = Math.min(fromAccountId, toAccountId);
        Long second = Math.max(fromAccountId, toAccountId);

        Account firstAccount = getAccount(first);
        Account secondAccount = getAccount(second);

        Account source = firstAccount.getId().equals(fromAccountId) ? firstAccount : secondAccount;
        Account destination = source == firstAccount ? secondAccount : firstAccount;

        if (!source.getOwnerId().equals(userId)) {
            throw new BadRequestException("Source account does not belong to user");
        }

        if (!source.getCurrency().equalsIgnoreCase(destination.getCurrency())) {
            throw new BadRequestException("Currency mismatch between accounts");
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromAccountId, source.getCurrency());
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        recordTransaction(source, TransactionType.TRANSFER_OUT, amount.negate(), "Transfer to " + toAccountId);
        recordTransaction(destination, TransactionType.TRANSFER_IN, amount, "Transfer from " + fromAccountId);
        auditService.record(userId, fromAccountId, "TRANSFER", "Transferred " + amount + " from " + fromAccountId + " to " + toAccountId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }

    private void recordTransaction(Account account, TransactionType type, BigDecimal amount, String description) {
        AccountTransaction tx = AccountTransaction.builder()
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .description(description)
                .build();
        transactionRepository.save(tx);
    }
}
