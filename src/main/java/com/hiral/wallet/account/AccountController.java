package com.hiral.wallet.account;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.hiral.wallet.idempotency.IdempotencyService;

import java.security.Principal;

@RestController
@RequestMapping("/api/accounts")
@Validated
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<Account> createAccount(Principal principal, @Valid @RequestBody CreateAccountRequest request) {
        Account created = accountService.createAccount(principal.getName(), request.getOwnerName(), request.getCurrency());
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(Principal principal, @PathVariable Long id) {
        Account account = accountService.getAccount(id);
        if (!account.getOwnerId().equals(principal.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(
            Principal principal,
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {

        Account updatedAccount = idempotencyService.execute(
                principal.getName(),
                idempotencyKey,
                "deposit",
                request,
                Account.class,
                () -> accountService.deposit(principal.getName(), id, request.getAmount(), request.getCurrency()));

        return ResponseEntity.ok(updatedAccount);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(
            Principal principal,
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request) {

        Account updatedAccount = idempotencyService.execute(
                principal.getName(),
                idempotencyKey,
                "withdraw",
                request,
                Account.class,
                () -> accountService.withdraw(principal.getName(), id, request.getAmount(), request.getCurrency()));

        return ResponseEntity.ok(updatedAccount);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            Principal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        idempotencyService.execute(
                principal.getName(),
                idempotencyKey,
                "transfer",
                request,
                Void.class,
                () -> {
                    accountService.transfer(principal.getName(), request.getFromAccountId(), request.getToAccountId(), request.getAmount());
                    return null;
                });

        return ResponseEntity.noContent().build();
    }
}
