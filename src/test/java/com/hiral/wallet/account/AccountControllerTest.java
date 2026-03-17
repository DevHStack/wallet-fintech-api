package com.hiral.wallet.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndQueryAccount_shouldReturnCreatedAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setOwnerName("Eve");

        String response = mockMvc.perform(post("/api/accounts")
                        .with(jwt().jwt(jwt -> jwt.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.balance").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Account created = objectMapper.readValue(response, Account.class);

        mockMvc.perform(get("/api/accounts/{id}", created.getId())
                        .with(jwt().jwt(jwt -> jwt.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Eve"));
    }
}
