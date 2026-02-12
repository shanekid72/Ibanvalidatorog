package com.pearldatadirect.ibanvalidator.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AeIbanValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  private record Payload(@AeIban String iban) {}

  @BeforeAll
  static void setup() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void acceptsValidAeIban_canonicalAndFormatted() {
    // Bank code 033 exists in the provided UAE participant list (e.g., Mashreq is 033).
    Iban valid = new Iban.Builder()
        .countryCode(CountryCode.AE)
        .bankCode("033")
        .accountNumber("1234567890123456")
        .build();

    // canonical
    assertTrue(validator.validate(new Payload(valid.toString())).isEmpty());

    // formatted (spaces) + lower-case input should still pass (we normalize)
    String formattedLower = valid.toFormattedString().toLowerCase();
    assertTrue(validator.validate(new Payload(formattedLower)).isEmpty());
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

    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(bad));
    assertFalse(violations.isEmpty());
    assertEquals("Invalid IBAN check digits", violations.iterator().next().getMessage());
  }

  @Test
  void rejectsNonAe() {
    String nonAe = "GB82WEST12345698765432";
    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(nonAe));
    assertFalse(violations.isEmpty());
    assertEquals("IBAN must start with AE", violations.iterator().next().getMessage());
  }

  @Test
  void rejectsBadCharacters() {
    String badChars = "AE07-0331-2345-6789-0123-456";
    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(badChars));
    assertFalse(violations.isEmpty());
    assertEquals("IBAN must be alphanumeric", violations.iterator().next().getMessage());
  }

  @Test
  void rejectsWrongLength() {
    String tooShort = "AE07033123456789012345"; // 22 chars
    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(tooShort));
    assertFalse(violations.isEmpty());
    assertEquals("UAE (AE) IBAN must be exactly 23 characters", violations.iterator().next().getMessage());
  }

  @Test
  void rejectsUnknownBankCode_evenIfChecksumIsValid() {
    // 999 is not in the provided list (max in the sheet is 809), but iban4j can still create a structurally valid AE IBAN.
    Iban iban = new Iban.Builder()
        .countryCode(CountryCode.AE)
        .bankCode("999")
        .accountNumber("1234567890123456")
        .build();

    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(iban.toString()));
    assertFalse(violations.isEmpty());
    assertEquals("Unknown UAE bank code", violations.iterator().next().getMessage());
  }
}
