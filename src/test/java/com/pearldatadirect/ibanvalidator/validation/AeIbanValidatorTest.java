package com.pearldatadirect.ibanvalidator.validation;

import com.pearldatadirect.ibanvalidator.registry.AeBankCodeRegistry;
import com.pearldatadirect.ibanvalidator.registry.AeBankInfo;
import jakarta.validation.ConstraintValidatorContext;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AeIbanValidatorTest {

  @Mock
  AeBankCodeRegistry registry;

  @Mock
  ConstraintValidatorContext context;

  @Mock
  ConstraintValidatorContext.ConstraintViolationBuilder builder;

  AeIbanValidator validator;

  @BeforeEach
  void setup() {
    validator = new AeIbanValidator(registry);
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
  }

  @Test
  void acceptsValidAeIban_canonicalAndFormatted() {
    // Mock the registry to accept "033"
    when(registry.get("033")).thenReturn(Optional.of(mock(AeBankInfo.class)));
    when(registry.isValid("033")).thenReturn(true);

    Iban valid = new Iban.Builder()
        .countryCode(CountryCode.AE)
        .bankCode("033")
        .accountNumber("1234567890123456")
        .build();

    // canonical
    assertTrue(validator.isValid(valid.toString(), context));

    // formatted (spaces) + lower-case input should still pass (we normalize)
    String formattedLower = valid.toFormattedString().toLowerCase();
    assertTrue(validator.isValid(formattedLower, context));
  }

  @Test
  void rejectsWrongChecksum() {
    Iban valid = new Iban.Builder()
        .countryCode(CountryCode.AE)
        .bankCode("033")
        .accountNumber("1234567890123456")
        .build();

    String s = valid.toString();
    // Flip last digit (simple checksum-break)
    char last = s.charAt(s.length() - 1);
    char flipped = (last == '9') ? '8' : '9';
    String bad = s.substring(0, s.length() - 1) + flipped;

    assertFalse(validator.isValid(bad, context));
    verify(context).buildConstraintViolationWithTemplate("Invalid IBAN check digits");
  }

  @Test
  void rejectsNonAe() {
    String nonAe = "GB82WEST12345698765432";
    assertFalse(validator.isValid(nonAe, context));
    verify(context).buildConstraintViolationWithTemplate("IBAN must start with AE");
  }

  @Test
  void rejectsBadCharacters() {
    String badChars = "AE07-0331-2345-6789-0123-456";
    assertFalse(validator.isValid(badChars, context));
    verify(context).buildConstraintViolationWithTemplate("IBAN must be alphanumeric");
  }

  @Test
  void rejectsWrongLength() {
    String tooShort = "AE07033123456789012345"; // 22 chars
    assertFalse(validator.isValid(tooShort, context));
    verify(context).buildConstraintViolationWithTemplate("UAE (AE) IBAN must be exactly 23 characters");
  }

  @Test
  void rejectsUnknownBankCode_evenIfChecksumIsValid() {
    // 999 is valid structurally but unknown in registry
    when(registry.get("999")).thenReturn(Optional.empty());

    Iban iban = new Iban.Builder()
        .countryCode(CountryCode.AE)
        .bankCode("999")
        .accountNumber("1234567890123456")
        .build();

    assertFalse(validator.isValid(iban.toString(), context));
    verify(context).buildConstraintViolationWithTemplate("Unknown UAE bank code");
  }
}
