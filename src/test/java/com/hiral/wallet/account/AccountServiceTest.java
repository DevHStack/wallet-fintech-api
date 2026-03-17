


package com.hiral.wallet.account;

import com.hiral.wallet.account.exception.InsufficientFundsException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    private Account accountA;
    private Account accountB;
    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        accountA = accountService.createAccount(USER_ID, "Alice", "EUR");
        accountB = accountService.createAccount(USER_ID, "Bob", "EUR");
    }

    @Test
    void createAccount_shouldInitializeWithZeroBalance() {
        assertThat(accountA.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deposit_shouldIncreaseBalance() {
        accountService.deposit(USER_ID, accountA.getId(), BigDecimal.valueOf(123.45), "EUR");

        Account updated = accountService.getAccount(accountA.getId());
        assertThat(updated.getBalance()).isEqualByComparingTo("123.45");
    }

    @Test
    void withdraw_shouldDecreaseBalance() {
        accountService.deposit(USER_ID, accountA.getId(), BigDecimal.valueOf(100), "EUR");
        accountService.withdraw(USER_ID, accountA.getId(), BigDecimal.valueOf(40), "EUR");

        Account updated = accountService.getAccount(accountA.getId());
        assertThat(updated.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void transfer_shouldMoveFundsBetweenAccounts() {
        accountService.deposit(USER_ID, accountA.getId(), BigDecimal.valueOf(200), "EUR");

        accountService.transfer(USER_ID, accountA.getId(), accountB.getId(), BigDecimal.valueOf(75));

        Account updatedA = accountService.getAccount(accountA.getId());
        Account updatedB = accountService.getAccount(accountB.getId());

        assertThat(updatedA.getBalance()).isEqualByComparingTo("125.00");
        assertThat(updatedB.getBalance()).isEqualByComparingTo("75.00");
    }

    @Test
    void withdraw_whenInsufficientFunds_shouldThrow() {
        assertThatThrownBy(() -> accountService.withdraw(USER_ID, accountA.getId(), BigDecimal.valueOf(10), "EUR"))
                .isInstanceOf(InsufficientFundsException.class);
    }
}
