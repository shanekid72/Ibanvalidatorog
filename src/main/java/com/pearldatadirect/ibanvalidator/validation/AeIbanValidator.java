package com.pearldatadirect.ibanvalidator.validation;

import com.pearldatadirect.ibanvalidator.registry.AeBankCodeRegistry;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.iban4j.IbanFormatException;
import org.iban4j.IbanUtil;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;

public final class AeIbanValidator implements ConstraintValidator<AeIban, String> {

  private final AeBankCodeRegistry bankCodeRegistry;

  public AeIbanValidator(AeBankCodeRegistry bankCodeRegistry) {
    this.bankCodeRegistry = bankCodeRegistry;
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return violate(context, "IBAN is required");
    }

    String iban = normalize(value);

    // Basic guards
    if (!iban.matches("^[A-Z0-9]+$")) {
      return violate(context, "IBAN must be alphanumeric");
    }

    // UAE-only gate: AE + 21 remaining chars (total 23)
    if (!iban.startsWith("AE")) {
      return violate(context, "IBAN must start with AE");
    }
    if (iban.length() != 23) {
      return violate(context, "UAE (AE) IBAN must be exactly 23 characters");
    }

    try {
      IbanUtil.validate(iban); // structure + checksum
    } catch (IbanFormatException e) {
      return violate(context, "Invalid IBAN format");
    } catch (InvalidCheckDigitException e) {
      return violate(context, "Invalid IBAN check digits");
    } catch (UnsupportedCountryException e) {
      return violate(context, "Unsupported IBAN country");
    }

    // Bank code validation: positions 4..6 (after country+check digits)
    String bankCode = iban.substring(4, 7);

    // If code missing entirely
    if (bankCodeRegistry.get(bankCode).isEmpty()) {
      return violate(context, "Unknown UAE bank code");
    }

    // If present but not valid per policy (e.g., non-Live when live-only enabled)
    if (!bankCodeRegistry.isValid(bankCode)) {
      return violate(context, "Inactive UAE bank code");
    }

    return true;
  }

  private static String normalize(String input) {
    return input.trim().toUpperCase().replaceAll("\\s+", "");
  }

  private static boolean violate(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    return false;
  }
}
