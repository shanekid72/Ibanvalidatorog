package com.pearldatadirect.ibanvalidator.web;

import com.pearldatadirect.ibanvalidator.registry.AeBankCodeRegistry;
import com.pearldatadirect.ibanvalidator.service.BankDetailsService;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BankDetailsController.class)
@Import(ApiExceptionHandler.class)
class BankDetailsControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  BankDetailsService service;

  @MockBean
  AeBankCodeRegistry registry;

  @Test
  void returns200OnValidRequest() throws Exception {
    Iban valid = new Iban.Builder()
        .countryCode(CountryCode.AE)
        .bankCode("033")
        .accountNumber("1234567890123456")
        .build();

    doNothing().when(service).add(org.mockito.ArgumentMatchers.any());

    // Configure mock registry to allow the IBAN to pass validation
    com.pearldatadirect.ibanvalidator.registry.AeBankInfo info = new com.pearldatadirect.ibanvalidator.registry.AeBankInfo(
        "033", "Mashreq", "Mashreq", "BOMLAEAD", "BOMLAEADXXX", "203320101", "Live");
    org.mockito.Mockito.when(registry.get("033")).thenReturn(java.util.Optional.of(info));
    org.mockito.Mockito.when(registry.isValid("033")).thenReturn(true);

    String body = "{\"iban\": \"" + valid.toFormattedString() + "\", \"accountHolderName\": \"Test User\"}";

    mockMvc.perform(post("/api/bank-details")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());
  }

  @Test
  void returns400WithProblemDetailAndErrorsMap() throws Exception {
    // Bad checksum and also has spaces; validator normalizes but should still
    // reject.
    String body = "{\"iban\": \"AE00 0000 0000 0000 0000 000\", \"accountHolderName\": \"Test User\"}";

    mockMvc.perform(post("/api/bank-details")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.iban").exists());
  }
}
