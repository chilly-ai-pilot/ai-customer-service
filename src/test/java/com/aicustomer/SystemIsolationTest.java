package com.aicustomer;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
class SystemIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * UC-12 / CT-07: Same account can exist in both User and CommercialTenant systems.
     * This verifies that the two tables (user_t and commercial_tenant_t) are independent.
     */
    @Test
    @DisplayName("UC-12 / CT-07: Same account exists in both User and CommercialTenant — no conflict")
    void test_same_account_in_both_systems() throws Exception {
        String sharedAccount = "shared_account";

        // 1. Register in User system
        RegisterRequest userReq = new RegisterRequest(sharedAccount, "pwd_user", "User Person");
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value(sharedAccount));

        // 2. Register same account in CommercialTenant system — should succeed
        RegisterRequest ctReq = new RegisterRequest(sharedAccount, "pwd_ct", "CT Business");
        mockMvc.perform(post("/commercialTenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value(sharedAccount));

        // 3. Login in User system with correct password
        LoginRequest userLogin = new LoginRequest(sharedAccount, "pwd_user");
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("User Person"));

        // 4. Login in CommercialTenant system with correct password
        LoginRequest ctLogin = new LoginRequest(sharedAccount, "pwd_ct");
        mockMvc.perform(post("/commercialTenant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ctLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("CT Business"));

        // 5. User system password should NOT work in CT system
        LoginRequest crossLogin = new LoginRequest(sharedAccount, "pwd_user");
        mockMvc.perform(post("/commercialTenant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }
}
