package com.hiral.wallet.account;

import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.hiral.wallet.account.DepositRequest;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/{id}/deposit")
public ResponseEntity<Account> deposit(
        @PathVariable Long id,
        @RequestBody DepositRequest request) {

    Account updatedAccount = accountService.deposit(id, request.getAmount());
    return ResponseEntity.ok(updatedAccount);
}
}
