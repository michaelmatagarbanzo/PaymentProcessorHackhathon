package com.ecommerce.sale.infrastructure.adapter.persistence;

import org.springframework.stereotype.Component;

/**
 * T047 - Servicio de enmascaramiento PAN.
 */
@Component
public class PanMaskingService {

    public String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank() || accountNumber.contains("*")) {
            return accountNumber;
        }
        if (accountNumber.length() <= 10) {
            return accountNumber;
        }
        int prefixLength = accountNumber.length() > 12 ? 8 : 6;
        int suffixLength = 4;
        int maskLength = Math.max(4, accountNumber.length() - prefixLength - suffixLength);

        StringBuilder masked = new StringBuilder();
        masked.append(accountNumber, 0, prefixLength);
        masked.append("*".repeat(maskLength));
        masked.append(accountNumber.substring(accountNumber.length() - suffixLength));
        return masked.toString();
    }
}