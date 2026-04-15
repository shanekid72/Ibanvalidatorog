# AE IBAN Validator

A Spring Boot REST API that validates **UAE (AE) IBAN** numbers and provides lookup/search over a curated registry of UAE banks. Built with **Spring Boot 3.5**, **Java 17**, and the **[iban4j](https://github.com/NayK4/iban4j)** library.

> **About the scope:** This service intentionally restricts validation to **UAE IBANs only**. The underlying `iban4j` dependency natively supports IBAN structure/checksum validation for **102 countries** — see the [Multi-country support](#multi-country-support-extending-beyond-uae) section below if you want to extend this project to other countries.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Running Locally](#running-locally)
- [Configuration](#configuration)
- [API Reference](#api-reference)
  - [POST /api/bank-details](#post-apibank-details) — UAE-strict
  - [GET /api/banks/{code}](#get-apibankscode) — UAE bank lookup
  - [GET /api/banks](#get-apibanks) — UAE bank search
  - [POST /api/iban/validate](#post-apiibanvalidate) — generic, all 102 countries
  - [GET /api/iban/validate](#get-apiibanvalidate) — generic, query-string variant
  - [GET /api/iban/countries](#get-apiibancountries) — list supported countries
- [How AE IBAN Validation Works](#how-ae-iban-validation-works)
- [UAE Bank Registry](#uae-bank-registry)
- [Multi-country Support (Extending Beyond UAE)](#multi-country-support-extending-beyond-uae)
- [Testing](#testing)
- [Swagger / OpenAPI](#swagger--openapi)
- [Troubleshooting](#troubleshooting)

---

## Features

- **Strict UAE IBAN validation** — enforces `AE` country code, exact 23-character length, ISO-13616 mod-97 checksum, and a known 3-digit UAE bank code
- **Generic multi-country IBAN validation** — `/api/iban/validate` supports all **102 IBAN countries** that iban4j knows about, returning parsed bank code, branch code, account number, etc.
- **Supported-countries listing** — `/api/iban/countries` returns every supported country with its ISO code, name, and IBAN length
- **UAE bank registry** — 48 banks loaded from CSV with bank code, participant name, BIC8/BIC11, routing number, and live/inactive status
- **Bank lookup endpoint** — fetch bank metadata by 3-digit code (e.g. `033` → Mashreq), with auto-padding (`33` → `033`)
- **Bank search endpoint** — substring match on name/short-name and exact-match on BIC
- **Live-only mode** (configurable) — reject IBANs whose bank is no longer "Live"
- **RFC 7807 ProblemDetail** error responses with field-level error map
- **OpenAPI 3 / Swagger UI** built in

---

## Tech Stack

| Layer            | Technology                                  |
| ---------------- | ------------------------------------------- |
| Language         | Java 17                                     |
| Framework        | Spring Boot 3.5.10 (spring-boot-starter-web)|
| Validation       | Jakarta Bean Validation + custom constraint |
| IBAN engine      | iban4j 3.2.11-RELEASE                       |
| API docs         | springdoc-openapi 2.8.5                     |
| Build            | Maven 3.8+                                  |
| Tests            | spring-boot-starter-test (JUnit 5, MockMvc) |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/pearldatadirect/ibanvalidator/
│   │   ├── AeIbanValidatorApplication.java     # Spring Boot main class
│   │   ├── api/
│   │   │   └── AddBankDetailsRequest.java      # Request DTO with @AeIban + @NotBlank
│   │   ├── config/
│   │   │   └── OpenApiConfig.java              # Swagger metadata
│   │   ├── registry/
│   │   │   ├── AeBankCodeRegistry.java         # Loads & queries the UAE bank CSV
│   │   │   └── AeBankInfo.java                 # Bank record (code, BIC, name, status)
│   │   ├── service/
│   │   │   └── BankDetailsService.java         # Business logic for POST /api/bank-details
│   │   ├── validation/
│   │   │   ├── AeIban.java                     # @AeIban annotation
│   │   │   └── AeIbanValidator.java            # ConstraintValidator implementation
│   │   └── web/
│   │       ├── ApiExceptionHandler.java        # Global @RestControllerAdvice → ProblemDetail
│   │       ├── BankDetailsController.java      # POST /api/bank-details (UAE-strict)
│   │       ├── BanksController.java            # GET /api/banks, GET /api/banks/{code}
│   │       └── IbanController.java             # /api/iban/* (generic, multi-country)
│   └── resources/
│       ├── application.yml                     # Port, app name, ae.bankcodes.live-only, springdoc paths
│       └── uae-bank-codes.csv                  # 48 UAE banks (source of truth)
└── test/
    └── java/com/pearldatadirect/ibanvalidator/
        ├── validation/AeIbanValidatorTest.java
        └── web/
            ├── BankDetailsControllerTest.java
            └── BanksControllerTest.java
```

---

## Requirements

- **JDK 17+** (tested with Microsoft OpenJDK 17.0.18)
- **Maven 3.8+** (or use the bundled wrapper if added later)
- An open port `8080` (configurable via `server.port`)

---

## Running Locally

### 1. Build

```bash
mvn clean package
```

This produces `target/ae-iban-validator-0.0.1-SNAPSHOT.jar`.

### 2. Run

Either:

```bash
mvn spring-boot:run
```

or:

```bash
java -jar target/ae-iban-validator-0.0.1-SNAPSHOT.jar
```

The service starts on **http://localhost:8080**.

### 3. Verify

```bash
curl http://localhost:8080/api/banks/033
# {"bankCode":"033","participant":"Mashreq","shortName":"Mashreq", ... }
```

---

## Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: ae-iban-validator

ae:
  bankcodes:
    live-only: true        # If true, only "Live" bank codes are accepted by the validator

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

| Property                  | Default | Description                                                                                                |
| ------------------------- | ------- | ---------------------------------------------------------------------------------------------------------- |
| `server.port`             | `8080`  | HTTP port                                                                                                  |
| `ae.bankcodes.live-only`  | `true`  | When `true`, validator rejects bank codes whose status is not `Live`. Set to `false` to allow all CSV codes |

You can override any of these via environment variable or command line:

```bash
java -jar app.jar --server.port=9090 --ae.bankcodes.live-only=false
```

---

## API Reference

### `POST /api/bank-details`

Validate a UAE IBAN and (in production code) persist the bank details. Returns `200 OK` on success, `400 Bad Request` with a ProblemDetail body on validation failure.

**Request body:**

```json
{
  "iban": "AE07 0331 2345 6789 0123 456",
  "accountHolderName": "Test User"
}
```

**Success response:** `200 OK` (empty body)

**Validation error response:** `400 Bad Request`

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "instance": "/api/bank-details",
  "errors": {
    "iban": "Invalid IBAN check digits"
  }
}
```

**Possible `iban` error messages:**

| Message                                       | Cause                                                            |
| --------------------------------------------- | ---------------------------------------------------------------- |
| `IBAN is required`                            | Field is null or blank                                           |
| `IBAN must be alphanumeric`                   | Contains characters outside `[A-Z0-9]` after normalization       |
| `IBAN must start with AE`                     | Country code is not `AE`                                         |
| `UAE (AE) IBAN must be exactly 23 characters` | Length is wrong                                                  |
| `Invalid IBAN format`                         | iban4j structure check failed                                    |
| `Invalid IBAN check digits`                   | Mod-97 checksum failed                                           |
| `Unknown UAE bank code`                       | Bank code (positions 4–6) is not in `uae-bank-codes.csv`         |
| `Inactive UAE bank code`                      | Bank code exists but its status is not `Live` and live-only=true |

**Curl example:**

```bash
curl -i -X POST http://localhost:8080/api/bank-details \
  -H "Content-Type: application/json" \
  -d '{"iban":"AE070331234567890123456","accountHolderName":"Test User"}'
```

> Spaces in the IBAN are trimmed and the value is normalized to uppercase before validation.

---

### `GET /api/banks/{code}`

Look up a UAE bank by its 3-digit IBAN bank code.

- Accepts 1–3 digit input — values are zero-padded (`33` → `033`).
- Returns `404 Not Found` if the code is unknown.

**Example:**

```bash
curl -s http://localhost:8080/api/banks/033 | jq
```

```json
{
  "bankCode": "033",
  "participant": "Mashreq",
  "shortName": "Mashreq",
  "bic8": "BOMLAEAD",
  "bic11": "BOMLAEADXXX",
  "routingNo": "203320101",
  "status": "Live"
}
```

---

### `GET /api/banks`

Search the UAE bank registry.

**Query parameters (all optional):**

| Param   | Type    | Default | Description                                                    |
| ------- | ------- | ------- | -------------------------------------------------------------- |
| `q`     | string  | —       | Case-insensitive substring match on `participant` + `shortName` |
| `bic`   | string  | —       | Exact match on `bic8` or `bic11` (case-insensitive)            |
| `limit` | integer | `50`    | Max results (clamped to `1..200`)                              |

By default, results respect the `ae.bankcodes.live-only` policy.

**Examples:**

```bash
# Substring search
curl -s "http://localhost:8080/api/banks?q=emirates&limit=10" | jq

# BIC lookup
curl -s "http://localhost:8080/api/banks?bic=BOMLAEAD" | jq

# Get up to 200 banks
curl -s "http://localhost:8080/api/banks?limit=200" | jq 'length'
```

---

### `POST /api/iban/validate`

**Generic, multi-country IBAN validator.** Unlike `/api/bank-details`, this endpoint is country-agnostic and supports every IBAN-using country known to iban4j (102 countries). It does **not** consult the UAE bank registry — only structure, length, and checksum.

Always returns `200 OK`. The `valid` field tells you whether the IBAN passed.

**Request:**

```json
{ "iban": "DE89 3704 0044 0532 0130 00" }
```

**Successful response:**

```json
{
  "valid": true,
  "iban": "DE89370400440532013000",
  "formatted": "DE89 3704 0044 0532 0130 00",
  "countryCode": "DE",
  "countryName": "Germany",
  "checkDigits": "89",
  "bban": "370400440532013000",
  "length": 22,
  "details": {
    "bankCode": "37040044",
    "branchCode": null,
    "accountNumber": "0532013000",
    "nationalCheckDigit": null,
    "accountType": null,
    "ownerAccountType": null,
    "identificationNumber": null
  },
  "errorType": null,
  "reason": null
}
```

**Failure response:**

```json
{
  "valid": false,
  "iban": "DE00370400440532013000",
  "formatted": null,
  "countryCode": "DE",
  "countryName": null,
  "checkDigits": null,
  "bban": null,
  "length": 22,
  "details": null,
  "errorType": "INVALID_CHECK_DIGITS",
  "reason": "Invalid IBAN check digits"
}
```

**`errorType` values:**

| `errorType`            | Meaning                                                       |
| ---------------------- | ------------------------------------------------------------- |
| `BLANK`                | IBAN string is null or blank                                  |
| `NON_ALPHANUMERIC`     | Contains characters outside `[A-Z0-9]` after normalization     |
| `UNSUPPORTED_COUNTRY`  | First two chars are not a supported IBAN country (e.g. `US`)   |
| `INVALID_CHECK_DIGITS` | Mod-97 checksum failed                                         |
| `INVALID_FORMAT`       | Wrong length, wrong character types in BBAN, etc.              |
| `INVALID`              | Catch-all for any other iban4j runtime exception               |

**Examples (curl):**

```bash
# UK (Great Britain)
curl -s -X POST http://localhost:8080/api/iban/validate \
  -H "Content-Type: application/json" \
  -d '{"iban":"GB82 WEST 1234 5698 7654 32"}'

# Saudi Arabia
curl -s -X POST http://localhost:8080/api/iban/validate \
  -H "Content-Type: application/json" \
  -d '{"iban":"SA0380000000608010167519"}'

# UAE — same as POST /api/bank-details but without bank-code lookup
curl -s -X POST http://localhost:8080/api/iban/validate \
  -H "Content-Type: application/json" \
  -d '{"iban":"AE070331234567890123456"}'
```

---

### `GET /api/iban/validate`

Convenience GET variant of the same validator. Useful for quick checks from a browser or shell.

**Query parameters:**

| Param  | Required | Description    |
| ------ | -------- | -------------- |
| `iban` | yes      | IBAN to validate |

```bash
curl -s "http://localhost:8080/api/iban/validate?iban=DE89370400440532013000"
```

Returns the same `ValidationResponse` shape as the POST endpoint.

---

### `GET /api/iban/countries`

Returns the full list of countries the underlying iban4j library can validate.

```bash
curl -s http://localhost:8080/api/iban/countries | jq 'length'
# 102
```

**Response (truncated):**

```json
[
  { "code": "AD", "name": "Andorra",              "ibanLength": 24 },
  { "code": "AE", "name": "United Arab Emirates", "ibanLength": 23 },
  { "code": "DE", "name": "Germany",              "ibanLength": 22 },
  { "code": "GB", "name": "United Kingdom",       "ibanLength": 22 },
  { "code": "SA", "name": "Saudi Arabia",         "ibanLength": 24 },
  ...
]
```

Use this endpoint to power UI dropdowns or to discover supported countries programmatically.

---

## How AE IBAN Validation Works

A UAE IBAN follows the structure:

```
AE | kk | bbb | nnnnnnnnnnnnnnnn
└─┬┘ └┬┘ └┬─┘ └────────┬────────┘
  │   │   │            └── 16-digit account number (BBAN)
  │   │   └──────────────── 3-digit bank code
  │   └──────────────────── 2-digit ISO-13616 check digits
  └──────────────────────── ISO-3166 country code (always "AE")
```

Total length: **23 characters**.

[`AeIbanValidator`](src/main/java/com/pearldatadirect/ibanvalidator/validation/AeIbanValidator.java) performs the following checks in order:

1. **Normalize** — trim, uppercase, strip whitespace.
2. **Alphanumeric check** — must match `^[A-Z0-9]+$`.
3. **Country gate** — must start with `AE`.
4. **Length check** — must be exactly 23 chars.
5. **Structure + checksum** — delegates to `org.iban4j.IbanUtil.validate(iban)`.
6. **Bank code lookup** — extracts positions 4..6 (`iban.substring(4, 7)`) and looks it up in [`AeBankCodeRegistry`](src/main/java/com/pearldatadirect/ibanvalidator/registry/AeBankCodeRegistry.java).
7. **Status check** — if `ae.bankcodes.live-only=true`, rejects non-Live banks.

Any failure short-circuits the rest and returns a specific error message via the `@AeIban` constraint.

---

## UAE Bank Registry

The registry is loaded once at startup from [`src/main/resources/uae-bank-codes.csv`](src/main/resources/uae-bank-codes.csv) — a curated list of **48 UAE banks** sourced from the original `Bank code.xlsx`.

**CSV schema:**

```
bank_code,participant,short_name,bic8,bic11,routing_no,status
003,Abu Dhabi Commercial Bank,ADCB,ADCBAEAA,ADCBAEAAXXX,600310101,Live
033,Mashreq,Mashreq,BOMLAEAD,BOMLAEADXXX,203320101,Live
...
```

To **add or update banks**, edit the CSV and restart the app. No code change required.

---

## Multi-country Support (Extending Beyond UAE)

The project ships with **two layers** of IBAN validation:

| Layer                 | Endpoint(s)                                          | Coverage             | Extras                                          |
| --------------------- | ---------------------------------------------------- | -------------------- | ----------------------------------------------- |
| **UAE-strict**        | `/api/bank-details`, `/api/banks/**`                 | UAE only             | Bank registry, BIC, routing number, live status |
| **Generic (iban4j)**  | `/api/iban/validate`, `/api/iban/countries`          | 102 countries        | Country code, BBAN, bank code, account number   |

The generic layer is implemented in [`IbanController`](src/main/java/com/pearldatadirect/ibanvalidator/web/IbanController.java) and delegates straight to `org.iban4j.IbanUtil.validate(iban)` + `org.iban4j.Iban.valueOf(iban)`. No registry maintenance is required to support a new country — iban4j already knows them.

### Countries supported out of the box (102)

`AD, AE, AL, AO, AT, AX, AZ, BA, BE, BG, BH, BI, BL, BR, BY, CH, CR, CV, CY, CZ, DE, DK, DO, EE, EG, ES, FI, FO, FR, GA, GB, GE, GF, GG, GI, GL, GP, GR, GT, HR, HU, IE, IL, IM, IQ, IR, IS, IT, JE, JO, KW, KZ, LB, LC, LI, LT, LU, LV, MA, MC, MD, ME, MF, MK, MQ, MR, MT, MU, MZ, NC, NL, NO, OM, PF, PK, PL, PM, PS, PT, QA, RE, RO, RS, RU, SA, SC, SE, SI, SK, SM, ST, SV, TF, TL, TN, TR, UA, VA, VG, WF, XK, YT`

Each has its own length (e.g., NO=15, FR=27, MT=31, RU=33) and BBAN structure that iban4j knows.

You can also fetch this list at runtime from `GET /api/iban/countries`.

### When to use which endpoint

- Use **`POST /api/iban/validate`** when you only need to know whether an arbitrary IBAN is structurally valid and want the parsed components (bank code, account number, etc.).
- Use **`POST /api/bank-details`** when you specifically need to validate a UAE IBAN against the curated UAE bank registry (e.g. reject inactive banks, attach bank metadata).

### Adding a richer per-country layer (optional)

If you want UAE-style bank metadata for another country, mirror the AE pattern:

1. Create `XxBankCodeRegistry`, `XxBankInfo`, `XxIban` annotation, `XxIbanValidator`.
2. Add an `xx-bank-codes.csv` resource.
3. Wire a new endpoint or route based on country code.

This is heavy work per country — only worth it for corridors where you genuinely need bank-level validation.

---

## Testing

Run the full test suite:

```bash
mvn test
```

Test files:

- [`AeIbanValidatorTest`](src/test/java/com/pearldatadirect/ibanvalidator/validation/AeIbanValidatorTest.java) — unit tests for the constraint validator
- [`BankDetailsControllerTest`](src/test/java/com/pearldatadirect/ibanvalidator/web/BankDetailsControllerTest.java) — `MockMvc` tests for the validation endpoint
- [`BanksControllerTest`](src/test/java/com/pearldatadirect/ibanvalidator/web/BanksControllerTest.java) — `MockMvc` tests for lookup and search

Tests use `iban4j`'s `Iban.Builder` to generate valid AE IBANs on the fly, so they don't depend on hardcoded sample IBANs.

---

## Swagger / OpenAPI

Once the app is running, interactive API docs are at:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

You can try every endpoint directly from the browser.

---

## Troubleshooting

**`Port 8080 was already in use`**
Another process is bound to 8080. Either kill it or set `--server.port=9090`.

**`mvn: command not found`**
Maven is not on your PATH. On Windows, install via `winget install Apache.Maven` or download the binary zip from https://maven.apache.org/download.cgi and add `bin/` to your PATH.

**`java: command not found` or "Unsupported class file major version"**
Install JDK 17. On Windows: `winget install Microsoft.OpenJDK.17`. Verify with `java -version`.

**`Unknown UAE bank code` for a real-looking IBAN**
The bank code (3 digits at positions 4–6) isn't in `uae-bank-codes.csv`. Either add it to the CSV or set `ae.bankcodes.live-only=false` if it's there but inactive.

**`Invalid IBAN check digits`**
The IBAN's 2-digit check (positions 2–3) doesn't satisfy the mod-97 rule. The IBAN was mistyped or fabricated. Use `iban4j`'s `Iban.Builder` to generate test values.

---

## License

This project does not yet declare a license. Add a `LICENSE` file if you intend to distribute or open-source it.
