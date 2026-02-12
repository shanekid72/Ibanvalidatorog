package com.pearldatadirect.ibanvalidator.registry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads UAE (AE) bank codes from src/main/resources/uae-bank-codes.csv
 *
 * File format: bank_code,participant,short_name,bic8,bic11,routing_no,status
 * bank_code must be a 3-digit string (e.g., 003, 033, 809).
 */
@Component
public class AeBankCodeRegistry {

  private final Map<String, AeBankInfo> byCode;
  private final boolean liveOnly;

  public AeBankCodeRegistry(@Value("${ae.bankcodes.live-only:true}") boolean liveOnly) {
    this.liveOnly = liveOnly;
    this.byCode = Collections.unmodifiableMap(load());
  }

  /**
   * Returns true if the bank code exists and (optionally) is Live.
   */
  public boolean isValid(String bankCode3Digits) {
    AeBankInfo info = byCode.get(bankCode3Digits);
    if (info == null) return false;
    if (!liveOnly) return true;
    return "Live".equalsIgnoreCase(safe(info.status()));
  }

  /**
   * Returns the raw bank info (even if inactive), if present in the CSV.
   */
  public Optional<AeBankInfo> get(String bankCode3Digits) {
    return Optional.ofNullable(byCode.get(bankCode3Digits));
  }

  public int size() {
    return byCode.size();
  }

  /**
   * Search bank metadata.
   *
   * @param q    substring match over participant + shortName (case-insensitive)
   * @param bic  exact match for BIC8 or BIC11 (case-insensitive)
   * @param limit max number of items to return
   */
  public List<AeBankInfo> search(String q, String bic, int limit) {
    String qn = safe(q).toLowerCase(Locale.ROOT);
    String bicN = safe(bic).toUpperCase(Locale.ROOT);

    return byCode.values().stream()
        // respect liveOnly policy
        .filter(info -> !liveOnly || "Live".equalsIgnoreCase(safe(info.status())))
        .filter(info -> {
          if (!bicN.isEmpty()) {
            return bicN.equalsIgnoreCase(safe(info.bic8())) || bicN.equalsIgnoreCase(safe(info.bic11()));
          }
          return true;
        })
        .filter(info -> {
          if (!qn.isEmpty()) {
            String hay = (safe(info.participant()) + " " + safe(info.shortName())).toLowerCase(Locale.ROOT);
            return hay.contains(qn);
          }
          return true;
        })
        .limit(limit)
        .collect(Collectors.toList());
  }

  private Map<String, AeBankInfo> load() {
    Map<String, AeBankInfo> map = new HashMap<>();
    ClassPathResource resource = new ClassPathResource("uae-bank-codes.csv");

    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String line;
      boolean header = true;
      while ((line = br.readLine()) != null) {
        if (header) {
          header = false;
          continue;
        }
        if (line.isBlank()) continue;

        // CSV is simple in this file (no embedded commas in any field)
        String[] p = line.split(",", -1);
        if (p.length < 7) continue;

        String code = p[0].trim();
        if (!code.matches("^\\d{3}$")) continue;

        AeBankInfo info = new AeBankInfo(
            code,
            p[1].trim(),
            p[2].trim(),
            p[3].trim(),
            p[4].trim(),
            p[5].trim(),
            p[6].trim()
        );

        map.putIfAbsent(code, info);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load UAE bank codes from uae-bank-codes.csv", e);
    }

    return map;
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }
}
