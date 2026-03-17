package com.hiral.wallet.account;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.hiral.wallet.idempotency.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

@RestController
@RequestMapping("/api/accounts")
@Validated
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management operations")
@SecurityRequirement(name = "bearer-jwt")
public class AccountController {

    private final AccountService accountService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Create a new account", description = "Creates a new wallet account for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Account> createAccount(Principal principal, @Valid @RequestBody CreateAccountRequest request) {
        Account created = accountService.createAccount(principal.getName(), request.getOwnerName(), request.getCurrency());
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account details", description = "Retrieves the account details for the specified ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "403", description = "Access denied - account does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Account> getAccount(Principal principal, @PathVariable Long id) {
        Account account = accountService.getAccount(id);
        if (!account.getOwnerId().equals(principal.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposit funds", description = "Deposits funds into the specified account. Use Idempotency-Key header to ensure idempotent requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit successful"),
            @ApiResponse(responseCode = "400", description = "Invalid deposit amount"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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
    @Operation(summary = "Withdraw funds", description = "Withdraws funds from the specified account. Use Idempotency-Key header to ensure idempotent requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Withdrawal successful"),
            @ApiResponse(responseCode = "400", description = "Invalid withdrawal amount or insufficient funds"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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
    @Operation(summary = "Transfer funds between accounts", description = "Transfers funds from one account to another. Both accounts must belong to the authenticated user. Use Idempotency-Key header to ensure idempotent requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transfer successful"),
            @ApiResponse(responseCode = "400", description = "Invalid transfer amount or insufficient funds"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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
