package com.hiral.wallet.account;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Account createAccount(String ownerName) {

        Account account = Account.builder()
                .ownerName(ownerName)
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }
    @Transactional
public Account deposit(Long accountId, BigDecimal amount) {

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new RuntimeException("Deposit amount must be positive");
    }

    Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));

    account.setBalance(account.getBalance().add(amount));

    return account;
}
}
