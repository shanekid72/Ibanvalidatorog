package com.pearldatadirect.ibanvalidator.registry;

/**
 * UAE bank metadata sourced from uae-bank-codes.csv
 */
public record AeBankInfo(
    String bankCode,
    String participant,
    String shortName,
    String bic8,
    String bic11,
    String routingNo,
    String status
) {}
