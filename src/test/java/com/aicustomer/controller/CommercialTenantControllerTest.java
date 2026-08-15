package com.aicustomer.controller;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.entity.CommercialTenant;
import com.aicustomer.repository.CommercialTenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommercialTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommercialTenantRepository commercialTenantRepository;

    @BeforeEach
    void setUp() {
        commercialTenantRepository.deleteAll();
    }

    // ==================== Register Tests ====================

    @Nested
    @DisplayName("POST /commercialTenant/register")
    class RegisterTests {

        @Test
        @DisplayName("商户注册成功 - 应返回 code=0 和商户信息")
        void register_Success() throws Exception {
            RegisterRequest request = new RegisterRequest("tenant001", "password123", "测试商户");

            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.account").value("tenant001"))
                    .andExpect(jsonPath("$.data.name").value("测试商户"))
                    .andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @DisplayName("重复商户注册 - 应返回 code=1001 账户已存在")
        void register_DuplicateAccount() throws Exception {
            // 先注册一个商户
            CommercialTenant existingTenant = new CommercialTenant();
            existingTenant.setAccount("existing_tenant");
            existingTenant.setPassword("hashed");
            existingTenant.setName("已存在商户");
            commercialTenantRepository.save(existingTenant);

            RegisterRequest request = new RegisterRequest("existing_tenant", "password123", "新商户");

            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1001))
                    .andExpect(jsonPath("$.message").value("账户已存在"));
        }

        @Test
        @DisplayName("空账号商户注册 - 应返回 code=1004 参数错误")
        void register_EmptyAccount() throws Exception {
            RegisterRequest request = new RegisterRequest("", "password123", "测试商户");

            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("账号不能为空"));
        }

        @Test
        @DisplayName("空密码商户注册 - 应返回 code=1004 参数错误")
        void register_EmptyPassword() throws Exception {
            RegisterRequest request = new RegisterRequest("tenant001", "", "测试商户");

            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }

        @Test
        @DisplayName("空名称商户注册 - 应返回 code=1004 参数错误")
        void register_EmptyName() throws Exception {
            RegisterRequest request = new RegisterRequest("tenant001", "password123", "");

            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("名称不能为空"));
        }
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("POST /commercialTenant/login")
    class LoginTests {

        @Test
        @DisplayName("商户登录成功 - 应返回 code=0 和商户信息")
        void login_Success() throws Exception {
            // 先注册
            RegisterRequest registerRequest = new RegisterRequest("tenant_login", "mypassword", "登录商户");
            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)));

            // 再登录
            LoginRequest loginRequest = new LoginRequest("tenant_login", "mypassword");

            mockMvc.perform(post("/commercialTenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.account").value("tenant_login"))
                    .andExpect(jsonPath("$.data.name").value("登录商户"));
        }

        @Test
        @DisplayName("商户密码错误 - 应返回 code=1002 密码错误")
        void login_WrongPassword() throws Exception {
            // 先注册
            RegisterRequest registerRequest = new RegisterRequest("tenant1", "correctpassword", "商户1");
            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)));

            // 用错误密码登录
            LoginRequest loginRequest = new LoginRequest("tenant1", "wrongpassword");

            mockMvc.perform(post("/commercialTenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1002))
                    .andExpect(jsonPath("$.message").value("密码错误"));
        }

        @Test
        @DisplayName("商户账户不存在 - 应返回 code=1003 账户不存在")
        void login_AccountNotFound() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent_tenant", "anypassword");

            mockMvc.perform(post("/commercialTenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1003))
                    .andExpect(jsonPath("$.message").value("账户不存在"));
        }

        @Test
        @DisplayName("商户使用明文hash值登录应失败 - 证明是输入哈希后比对")
        void login_HashValueMustFail() throws Exception {
            // 先注册
            RegisterRequest registerRequest = new RegisterRequest("hash_tenant", "password123", "哈希商户");
            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)));

            // 用密码的SHA-256 hash值登录，应该失败
            // 系统会对输入 "5e884898..." 再次hash，所以匹配不上
            LoginRequest loginRequest = new LoginRequest("hash_tenant",
                    "5e884898da28047d9164f6e7360743f6a12a6b7aeabfe91c1a9e8c3a6b3e7c1a");

            mockMvc.perform(post("/commercialTenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1002))
                    .andExpect(jsonPath("$.message").value("密码错误"));
        }

        @Test
        @DisplayName("空账号商户登录 - 应返回 code=1004 参数错误")
        void login_EmptyAccount() throws Exception {
            LoginRequest loginRequest = new LoginRequest("", "password123");

            mockMvc.perform(post("/commercialTenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("账号不能为空"));
        }

        @Test
        @DisplayName("空密码商户登录 - 应返回 code=1004 参数错误")
        void login_EmptyPassword() throws Exception {
            LoginRequest loginRequest = new LoginRequest("tenant001", "");

            mockMvc.perform(post("/commercialTenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }
    }
}
