package com.hiral.wallet.account.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    List<AccountTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
