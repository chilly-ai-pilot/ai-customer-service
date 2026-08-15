package com.aicustomer;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
class CommercialTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CT_BASE = "/commercialTenant";

    // ─────────────────────────────────────────────────────────
    // CommercialTenant Registration
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CT-01: CommercialTenant registration success")
    void test01_register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("ct_testuser", "password123", "Test Tenant");

        mockMvc.perform(post(CT_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value("ct_testuser"))
                .andExpect(jsonPath("$.data.name").value("Test Tenant"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("CT-02: CommercialTenant registration duplicate — account already exists")
    void test02_register_duplicate() throws Exception {
        RegisterRequest request = new RegisterRequest("ct_testuser", "password123", "Test Tenant");

        // First registration
        mockMvc.perform(post(CT_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Duplicate registration should fail with code 1001
        mockMvc.perform(post(CT_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("账号已存在"));
    }

    // ─────────────────────────────────────────────────────────
    // CommercialTenant Login
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CT-03: CommercialTenant login success with correct password")
    void test03_login_success() throws Exception {
        // Register first
        RegisterRequest registerReq = new RegisterRequest("ct_login_ok", "mypassword", "Login Tenant");
        mockMvc.perform(post(CT_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Login
        LoginRequest loginReq = new LoginRequest("ct_login_ok", "mypassword");
        mockMvc.perform(post(CT_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value("ct_login_ok"))
                .andExpect(jsonPath("$.data.name").value("Login Tenant"));
    }

    @Test
    @DisplayName("CT-04: CommercialTenant login failure with wrong password")
    void test04_login_wrong_password() throws Exception {
        // Register
        RegisterRequest registerReq = new RegisterRequest("ct_wrong_pwd", "correctpwd", "Wrong Pwd Tenant");
        mockMvc.perform(post(CT_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Login with wrong password
        LoginRequest loginReq = new LoginRequest("ct_wrong_pwd", "wrongpwd");
        mockMvc.perform(post(CT_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    @Test
    @DisplayName("CT-05: CommercialTenant login failure with non-existent account")
    void test05_login_account_not_found() throws Exception {
        LoginRequest loginReq = new LoginRequest("nonexistent_ct_xyz", "anypassword");
        mockMvc.perform(post(CT_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("账号不存在"));
    }

    @Test
    @DisplayName("CT-06: CommercialTenant login with SHA-256 hash value must fail")
    void test06_login_hashed_value_fails() throws Exception {
        // Register
        RegisterRequest registerReq = new RegisterRequest("ct_hash_check", "secret456", "Hash Check Tenant");
        mockMvc.perform(post(CT_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Passing the SHA-256 hash of "secret456" should fail
        LoginRequest loginReq = new LoginRequest("ct_hash_check", "a3c0e5d1f6b7c8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3");
        mockMvc.perform(post(CT_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }
}
