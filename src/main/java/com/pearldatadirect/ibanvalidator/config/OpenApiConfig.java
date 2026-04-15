package com.pearldatadirect.ibanvalidator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ibanValidatorOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("IBAN Validator API")
                        .description("""
                                Two layers of IBAN validation:

                                • **UAE-strict** (`/api/bank-details`, `/api/banks/**`) — validates UAE IBANs against a curated registry of 48 UAE banks, with BIC, routing number, and live/inactive status.

                                • **Generic, multi-country** (`/api/iban/**`) — country-agnostic validation backed by iban4j, supporting 102 IBAN-using countries with parsed bank code, branch code, and account number.
                                """)
                        .version("1.1"));
    }
}
