package com.pearldatadirect.ibanvalidator.web;

import com.pearldatadirect.ibanvalidator.api.AddBankDetailsRequest;
import com.pearldatadirect.ibanvalidator.service.BankDetailsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank-details")
public class BankDetailsController {

  private final BankDetailsService service;

  public BankDetailsController(BankDetailsService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<Void> add(@RequestBody @Valid AddBankDetailsRequest req) {
    service.add(req);
    return ResponseEntity.ok().build();
  }
}
