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
                .info(new Info().title("UAE IBAN Validator API")
                        .description("API for validating UAE IBANs and searching bank codes.")
                        .version("1.0"));
    }
}
