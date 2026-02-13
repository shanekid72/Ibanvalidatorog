package com.pearldatadirect.ibanvalidator.web;

import com.pearldatadirect.ibanvalidator.registry.AeBankCodeRegistry;
import com.pearldatadirect.ibanvalidator.registry.AeBankInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BanksController.class)
class BanksControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  AeBankCodeRegistry registry;

  @Test
  void returns200WhenFound() throws Exception {
    AeBankInfo info = new AeBankInfo("033", "Mashreq", "Mashreq", "BOMLAEAD", "BOMLAEADXXX", "203320101", "Live");
    when(registry.get("033")).thenReturn(Optional.of(info));

    mockMvc.perform(get("/api/banks/33"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bankCode").value("033"))
        .andExpect(jsonPath("$.participant").value("Mashreq"));
  }

  @Test
  void returns404WhenNotFound() throws Exception {
    when(registry.get("999")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/banks/999"))
        .andExpect(status().isNotFound());
  }

  @Test
  void searchReturnsList() throws Exception {
    AeBankInfo info = new AeBankInfo("033", "Mashreq", "Mashreq", "BOMLAEAD", "BOMLAEADXXX", "203320101", "Live");
    when(registry.search("mash", null, 50)).thenReturn(new ArrayList<>(List.of(info)));

    mockMvc.perform(get("/api/banks?q=mash"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].bankCode").value("033"));
  }

  @Test
  void searchByBicReturnsList() throws Exception {
    AeBankInfo info = new AeBankInfo("033", "Mashreq", "Mashreq", "BOMLAEAD", "BOMLAEADXXX", "203320101", "Live");
    when(registry.search(null, "BOMLAEAD", 50)).thenReturn(new ArrayList<>(List.of(info)));

    mockMvc.perform(get("/api/banks?bic=BOMLAEAD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].bic8").value("BOMLAEAD"));
  }
}
