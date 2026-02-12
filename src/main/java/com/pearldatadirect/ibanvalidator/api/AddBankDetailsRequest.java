package com.pearldatadirect.ibanvalidator.api;

import com.pearldatadirect.ibanvalidator.validation.AeIban;
import jakarta.validation.constraints.NotBlank;

public record AddBankDetailsRequest(
    @NotBlank(message = "IBAN is required")
    @AeIban
    String iban,

    @NotBlank(message = "Account holder name is required")
    String accountHolderName
) {}
