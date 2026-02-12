package com.pearldatadirect.ibanvalidator.service;

import com.pearldatadirect.ibanvalidator.api.AddBankDetailsRequest;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;

@Service
public class BankDetailsService {

  /**
   * In a real system you'd persist to a DB and avoid logging sensitive data.
   * Here we just parse + return normally to demonstrate backend validation & extraction.
   */
  public void add(AddBankDetailsRequest req) {
    String canonical = canonicalize(req.iban());
    Iban iban = Iban.valueOf(canonical);

    // Example: extract parts if you need them downstream
    String bankCode = iban.getBankCode();
    String accountNumber = iban.getAccountNumber();

    // TODO: persist canonical IBAN + optional derived attributes (bankCode, accountNumber)
    // No-op for this sample.
  }

  public static String canonicalize(String ibanInput) {
    return ibanInput == null ? null : ibanInput.trim().toUpperCase().replaceAll("\s+", "");
  }
}
