package com.pearldatadirect.ibanvalidator.web;

import com.pearldatadirect.ibanvalidator.registry.AeBankCodeRegistry;
import com.pearldatadirect.ibanvalidator.registry.AeBankInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/banks")
public class BanksController {

  private final AeBankCodeRegistry registry;

  public BanksController(AeBankCodeRegistry registry) {
    this.registry = registry;
  }

  /**
   * Lookup a UAE bank by its 3-digit bank code as used in AE IBANs.
   *
   * Examples:
   *  - /api/banks/033
   *  - /api/banks/33  (will be normalized to 033)
   */
  @GetMapping("{code}")
  public ResponseEntity<AeBankInfo> getByCode(@PathVariable String code) {
    String normalized = normalizeCode(code);

    return registry.get(normalized)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bank code not found"));
  }

  /**
   * Search UAE banks.
   *
   * Query options (any may be provided):
   *  - q:   substring match over participant + shortName (case-insensitive)
   *  - bic: exact match for BIC8 or BIC11 (case-insensitive)
   *
   * By default, results are filtered by the registry policy:
   *  - ae.bankcodes.live-only=true  => returns only "Live" banks
   *  - ae.bankcodes.live-only=false => returns all banks in the CSV
   */
  @GetMapping
  public ResponseEntity<List<AeBankInfo>> search(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "bic", required = false) String bic,
      @RequestParam(name = "limit", required = false, defaultValue = "50") int limit
  ) {
    int cappedLimit = Math.max(1, Math.min(limit, 200));

    List<AeBankInfo> results = registry.search(q, bic, cappedLimit);
    results.sort(Comparator.comparing(AeBankInfo::bankCode));
    return ResponseEntity.ok(results);
  }

  private static String normalizeCode(String code) {
    if (code == null) return "";
    String c = code.trim();
    if (!c.matches("^\\d{1,3}$")) {
      return c; // will 404
    }
    return String.format("%03d", Integer.parseInt(c));
  }
}
