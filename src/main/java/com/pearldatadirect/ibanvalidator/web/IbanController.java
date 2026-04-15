package com.pearldatadirect.ibanvalidator.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanFormatException;
import org.iban4j.IbanUtil;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.iban4j.bban.BbanStructure;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Generic, multi-country IBAN validation endpoints.
 *
 * Unlike {@link BankDetailsController} (which is restricted to UAE IBANs and looks up
 * bank metadata in a curated registry), this controller delegates straight to iban4j
 * and supports every IBAN-using country the library knows about (~102 countries).
 *
 * Use this when you need structure + checksum validation only, without country-specific
 * bank lookup.
 */
@RestController
@RequestMapping("/api/iban")
@Tag(name = "IBAN (generic, multi-country)", description = "Country-agnostic IBAN validation backed by iban4j")
public class IbanController {

  public record ValidateRequest(String iban) {}

  public record IbanDetails(
      String bankCode,
      String branchCode,
      String accountNumber,
      String nationalCheckDigit,
      String accountType,
      String ownerAccountType,
      String identificationNumber
  ) {}

  public record ValidationResponse(
      boolean valid,
      String iban,                 // canonical (uppercase, no spaces)
      String formatted,            // grouped in 4-char blocks, e.g. "AE07 0331 ..."
      String countryCode,          // ISO-3166 alpha-2, e.g. "AE"
      String countryName,          // e.g. "United Arab Emirates"
      String checkDigits,
      String bban,
      Integer length,
      IbanDetails details,         // null on failure
      String errorType,            // null on success
      String reason                // null on success
  ) {}

  @Operation(summary = "Validate an IBAN (any supported country)")
  @PostMapping("/validate")
  public ResponseEntity<ValidationResponse> validatePost(@RequestBody ValidateRequest req) {
    return ResponseEntity.ok(validate(req == null ? null : req.iban()));
  }

  @Operation(summary = "Validate an IBAN via query string (any supported country)")
  @GetMapping("/validate")
  public ResponseEntity<ValidationResponse> validateGet(@RequestParam("iban") String iban) {
    return ResponseEntity.ok(validate(iban));
  }

  @Operation(summary = "List ISO country codes supported by the underlying IBAN library")
  @GetMapping("/countries")
  public ResponseEntity<List<Map<String, Object>>> supportedCountries() {
    List<Map<String, Object>> out = BbanStructure.supportedCountries().stream()
        .sorted(Comparator.comparing(CountryCode::name))
        .map(c -> {
          Map<String, Object> m = new java.util.LinkedHashMap<>();
          m.put("code", c.name());
          m.put("name", c.getName());
          m.put("ibanLength", BbanStructure.forCountry(c).getBbanLength() + 4);
          return m;
        })
        .toList();
    return ResponseEntity.ok(out);
  }

  // ---- internals ----

  private ValidationResponse validate(String raw) {
    if (raw == null || raw.isBlank()) {
      return failure(raw, "BLANK", "IBAN is required");
    }

    String canonical = raw.trim().toUpperCase().replaceAll("\\s+", "");

    if (!canonical.matches("^[A-Z0-9]+$")) {
      return failure(canonical, "NON_ALPHANUMERIC", "IBAN must be alphanumeric");
    }

    try {
      IbanUtil.validate(canonical); // structure + length + checksum
    } catch (UnsupportedCountryException e) {
      return failure(canonical, "UNSUPPORTED_COUNTRY",
          "Country code '" + safeCountry(canonical) + "' is not a recognised IBAN country");
    } catch (InvalidCheckDigitException e) {
      return failure(canonical, "INVALID_CHECK_DIGITS", "Invalid IBAN check digits");
    } catch (IbanFormatException e) {
      return failure(canonical, "INVALID_FORMAT",
          e.getMessage() == null ? "Invalid IBAN format" : e.getMessage());
    } catch (RuntimeException e) {
      return failure(canonical, "INVALID", e.getMessage());
    }

    Iban iban = Iban.valueOf(canonical);
    CountryCode cc = iban.getCountryCode();

    IbanDetails details = new IbanDetails(
        nullIfBlank(iban.getBankCode()),
        nullIfBlank(iban.getBranchCode()),
        nullIfBlank(iban.getAccountNumber()),
        nullIfBlank(iban.getNationalCheckDigit()),
        nullIfBlank(iban.getAccountType()),
        nullIfBlank(iban.getOwnerAccountType()),
        nullIfBlank(iban.getIdentificationNumber())
    );

    return new ValidationResponse(
        true,
        canonical,
        iban.toFormattedString(),
        cc.name(),
        cc.getName(),
        iban.getCheckDigit(),
        iban.getBban(),
        canonical.length(),
        details,
        null,
        null
    );
  }

  private static ValidationResponse failure(String canonical, String type, String reason) {
    return new ValidationResponse(
        false,
        canonical,
        null,
        canonical != null && canonical.length() >= 2 ? canonical.substring(0, 2) : null,
        null,
        null,
        null,
        canonical == null ? null : canonical.length(),
        null,
        type,
        reason
    );
  }

  private static String safeCountry(String canonical) {
    return canonical.length() >= 2 ? canonical.substring(0, 2) : canonical;
  }

  private static String nullIfBlank(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }
}
