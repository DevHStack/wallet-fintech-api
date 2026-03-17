package com.hiral.wallet.currency;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyConversionService {

    private static final Map<String, BigDecimal> RATE_TO_EUR = Map.of(
            "EUR", BigDecimal.ONE,
            "USD", BigDecimal.valueOf(0.92),
            "GBP", BigDecimal.valueOf(1.15)
    );

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal fromRate = RATE_TO_EUR.getOrDefault(fromCurrency.toUpperCase(), BigDecimal.ONE);
        BigDecimal toRate = RATE_TO_EUR.getOrDefault(toCurrency.toUpperCase(), BigDecimal.ONE);
        return amount.multiply(fromRate).divide(toRate, 4, RoundingMode.HALF_UP);
    }

    public boolean isSupported(String currency) {
        return currency != null && RATE_TO_EUR.containsKey(currency.toUpperCase());
    }
}
