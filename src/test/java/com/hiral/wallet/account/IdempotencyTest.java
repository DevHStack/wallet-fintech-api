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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deposit_withSameIdempotencyKey_isAppliedOnce() throws Exception {
        CreateAccountRequest create = new CreateAccountRequest();
        create.setOwnerName("Eve");

        String createResponse = mockMvc.perform(post("/api/accounts")
                        .with(jwt().jwt(jwt2 -> jwt2.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Account account = objectMapper.readValue(createResponse, Account.class);

        DepositRequest deposit = new DepositRequest();
        deposit.setAmount(BigDecimal.valueOf(100));

        String key = "test-key-123";

        mockMvc.perform(post("/api/accounts/{id}/deposit", account.getId())
                        .with(jwt().jwt(jwt2 -> jwt2.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));

        // Reusing same key should not double deposit
        mockMvc.perform(post("/api/accounts/{id}/deposit", account.getId())
                        .with(jwt().jwt(jwt2 -> jwt2.subject("user-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100));
    }
}
