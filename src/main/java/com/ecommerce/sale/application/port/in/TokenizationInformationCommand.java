package com.ecommerce.sale.application.port.in;

/**
 * Comando de entrada para datos de tokenización (wallets, criptograma).
 */
public record TokenizationInformationCommand(
    String wallet,
    String device,
    String paymentIndicator,
    String cryptogramEci,
    String cryptogram
) {}
