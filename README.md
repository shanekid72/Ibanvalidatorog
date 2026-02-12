# AE IBAN Validator (Spring Boot + iban4j)

A small Spring Boot backend project that validates **UAE (AE) IBAN** values using **iban4j** and Jakarta Bean Validation.

## Requirements
- Java 17+
- Maven 3.8+

## Run locally
```bash
mvn clean test
mvn spring-boot:run
```

Service runs on: http://localhost:8080

## API

### POST /api/bank-details

Example (validates AE-only IBAN):

```bash
curl -i -X POST http://localhost:8080/api/bank-details \
  -H "Content-Type: application/json" \
  -d '{ "iban": "AE07 0331 2345 6789 0123 456", "accountHolderName": "Test User" }'
```

Validation errors are returned as HTTP 400 using ProblemDetail with an `errors` map.

## Notes
- The server stores **canonical IBAN** form (uppercase, no spaces).
- For tests, we generate an AE IBAN using iban4j's `Iban.Builder`.


## UAE bank code validation
This project loads UAE bank codes from `src/main/resources/uae-bank-codes.csv` (generated from your `Bank code.xlsx`).

- Total codes loaded: **48**
- Validation rejects structurally valid AE IBANs if the **3-digit bank code** is not in the list.


## Bank code lookup
Lookup bank metadata by the 3-digit UAE bank code used inside AE IBANs:

```bash
curl -s http://localhost:8080/api/banks/033 | jq
curl -s http://localhost:8080/api/banks/33  | jq   # normalizes to 033
```

## Live-only validation mode
By default, validation allows only bank codes whose status is `Live`.

You can disable this (allow all codes in the CSV) by setting:

```yaml
ae:
  bankcodes:
    live-only: false
```


### Search bank metadata
```bash
curl -s "http://localhost:8080/api/banks?q=mash" | jq
curl -s "http://localhost:8080/api/banks?bic=BOMLAEAD" | jq
curl -s "http://localhost:8080/api/banks?limit=10" | jq
```
