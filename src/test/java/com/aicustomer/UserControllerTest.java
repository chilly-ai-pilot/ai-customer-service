package com.aicustomer;
import static com.aicustomer.constant.ErrorCodes.*;
import com.aicustomer.constant.ErrorCodes;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.MethodName.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_BASE = "/user";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up before each test to ensure idempotency
        try {
            mockMvc.perform(delete(USER_BASE + "/cleanup/testuser_user"));
        } catch (Exception ignored) {
        }
        try {
            mockMvc.perform(delete(USER_BASE + "/cleanup/testuser_same"));
        } catch (Exception ignored) {
        }
    }

    // ─────────────────────────────────────────────────────────
    // User Registration
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("UC-01: User registration success")
    void test01_register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser_user", "password123", "Test User");

        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value("testuser_user"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("UC-02: User registration duplicate — account already exists")
    void test02_register_duplicate() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser_user", "password123", "Test User");

        // First registration
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Duplicate registration should fail with code 1001
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ACCOUNT_ALREADY_EXISTS))
                .andExpect(jsonPath("$.message").value(ErrorCodes.MSG_ACCOUNT_ALREADY_EXISTS));
    }

    // ─────────────────────────────────────────────────────────
    // User Login
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("UC-03: User login success with correct password")
    void test03_login_success() throws Exception {
        // Register first
        RegisterRequest registerReq = new RegisterRequest("testuser_login_ok", "mypassword", "Login User");
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Login with correct password
        LoginRequest loginReq = new LoginRequest("testuser_login_ok", "mypassword");
        mockMvc.perform(post(USER_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.account").value("testuser_login_ok"))
                .andExpect(jsonPath("$.data.name").value("Login User"));
    }

    @Test
    @DisplayName("UC-04: User login failure with wrong password")
    void test04_login_wrong_password() throws Exception {
        // Register
        RegisterRequest registerReq = new RegisterRequest("testuser_wrong_pwd", "correctpwd", "Wrong Pwd User");
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Login with wrong password
        LoginRequest loginReq = new LoginRequest("testuser_wrong_pwd", "wrongpwd");
        mockMvc.perform(post(USER_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ACCOUNT_NOT_FOUND))
                .andExpect(jsonPath("$.message").value(ErrorCodes.MSG_PASSWORD_ERROR));
    }

    @Test
    @DisplayName("UC-05: User login failure with non-existent account")
    void test05_login_account_not_found() throws Exception {
        LoginRequest loginReq = new LoginRequest("nonexistent_account_xyz", "anypassword");
        mockMvc.perform(post(USER_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PASSWORD_ERROR))
                .andExpect(jsonPath("$.message").value(ErrorCodes.MSG_ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("UC-06: User login with SHA-256 hash value must fail (proves hashing, not string compare)")
    void test06_login_hashed_value_fails() throws Exception {
        // Register
        RegisterRequest registerReq = new RegisterRequest("testuser_hash_check", "secret123", "Hash Check User");
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // The SHA-256 hash of "secret123" is NOT the literal string "secret123".
        // Passing the hash string should fail authentication.
        LoginRequest loginReq = new LoginRequest("testuser_hash_check", "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8");
        mockMvc.perform(post(USER_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.ACCOUNT_NOT_FOUND))
                .andExpect(jsonPath("$.message").value(ErrorCodes.MSG_PASSWORD_ERROR));
    }

    // ─────────────────────────────────────────────────────────
    // Parameter Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("UC-07: Registration with empty account returns code 1004 (not 500)")
    void test07_register_empty_account_returns_1004() throws Exception {
        RegisterRequest request = new RegisterRequest("", "password123", "Test User");
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PARAMETER_ERROR));
    }

    @Test
    @DisplayName("UC-08: Registration with empty password returns code 1004 (not 500)")
    void test08_register_empty_password_returns_1004() throws Exception {
        RegisterRequest request = new RegisterRequest("someuser", "", "Test User");
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PARAMETER_ERROR));
    }

    @Test
    @DisplayName("UC-09: Registration with empty name returns code 1004 (not 500)")
    void test09_register_empty_name_returns_1004() throws Exception {
        RegisterRequest request = new RegisterRequest("someuser", "password123", "");
        mockMvc.perform(post(USER_BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PARAMETER_ERROR));
    }

    @Test
    @DisplayName("UC-10: Login with empty account returns code 1004 (not 500)")
    void test10_login_empty_account_returns_1004() throws Exception {
        LoginRequest request = new LoginRequest("", "password123");
        mockMvc.perform(post(USER_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PARAMETER_ERROR));
    }

    @Test
    @DisplayName("UC-11: Login with empty password returns code 1004 (not 500)")
    void test11_login_empty_password_returns_1004() throws Exception {
        LoginRequest request = new LoginRequest("someuser", "");
        mockMvc.perform(post(USER_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodes.PARAMETER_ERROR));
    }
}
