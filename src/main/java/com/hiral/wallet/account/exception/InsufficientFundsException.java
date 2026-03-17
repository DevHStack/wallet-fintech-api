package com.hiral.wallet.account.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId, String currency) {
        super("Insufficient funds in account " + accountId + " (" + currency + ")");
    }
}
