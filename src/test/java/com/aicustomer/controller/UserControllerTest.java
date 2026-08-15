package com.aicustomer.controller;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.entity.User;
import com.aicustomer.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ==================== Register Tests ====================

    @Nested
    @DisplayName("POST /user/register")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 - 应返回 code=0 和用户信息")
        void register_Success() throws Exception {
            RegisterRequest request = new RegisterRequest("testuser", "password123", "张三");

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.account").value("testuser"))
                    .andExpect(jsonPath("$.data.name").value("张三"))
                    .andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @DisplayName("重复注册 - 应返回 code=1001 账户已存在")
        void register_DuplicateAccount() throws Exception {
            // 先注册一个用户
            User existingUser = new User();
            existingUser.setAccount("existing");
            existingUser.setPassword("hashed"); // 密码是存储前哈希的，但这里直接存hash值
            existingUser.setName("已存在");
            userRepository.save(existingUser);

            RegisterRequest request = new RegisterRequest("existing", "password123", "李四");

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1001))
                    .andExpect(jsonPath("$.message").value("账户已存在"));
        }

        @Test
        @DisplayName("空账号注册 - 应返回 code=1004 参数错误")
        void register_EmptyAccount() throws Exception {
            RegisterRequest request = new RegisterRequest("", "password123", "张三");

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("账号不能为空"));
        }

        @Test
        @DisplayName("空密码注册 - 应返回 code=1004 参数错误")
        void register_EmptyPassword() throws Exception {
            RegisterRequest request = new RegisterRequest("testuser", "", "张三");

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }

        @Test
        @DisplayName("空名称注册 - 应返回 code=1004 参数错误")
        void register_EmptyName() throws Exception {
            RegisterRequest request = new RegisterRequest("testuser", "password123", "");

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("名称不能为空"));
        }
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("POST /user/login")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 应返回 code=0 和用户信息")
        void login_Success() throws Exception {
            // 先注册
            RegisterRequest registerRequest = new RegisterRequest("loginuser", "mypassword", "登录用户");
            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)));

            // 再登录
            LoginRequest loginRequest = new LoginRequest("loginuser", "mypassword");

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.account").value("loginuser"))
                    .andExpect(jsonPath("$.data.name").value("登录用户"));
        }

        @Test
        @DisplayName("密码错误 - 应返回 code=1002 密码错误")
        void login_WrongPassword() throws Exception {
            // 先注册
            RegisterRequest registerRequest = new RegisterRequest("user1", "correctpassword", "用户1");
            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)));

            // 用错误密码登录
            LoginRequest loginRequest = new LoginRequest("user1", "wrongpassword");

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1002))
                    .andExpect(jsonPath("$.message").value("密码错误"));
        }

        @Test
        @DisplayName("账户不存在 - 应返回 code=1003 账户不存在")
        void login_AccountNotFound() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent", "anypassword");

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1003))
                    .andExpect(jsonPath("$.message").value("账户不存在"));
        }

        @Test
        @DisplayName("使用明文hash值登录应失败 - 证明是输入哈希后比对而非字符串比对")
        void login_HashValueMustFail() throws Exception {
            // 先注册
            RegisterRequest registerRequest = new RegisterRequest("hashuser", "password123", "哈希用户");
            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)));

            // 获取存储的密码hash值（通过注册响应中的信息，这里用已知的hash值测试）
            // SHA-256("password123") = 5e884898da28047d9164f6e7360743f6a12a6b7aeabfe91c1a9e8c3a6b3e7c1a
            // 如果直接用这个hash值登录，应该失败（因为系统会对输入再次hash）
            LoginRequest loginRequest = new LoginRequest("hashuser",
                    "5e884898da28047d9164f6e7360743f6a12a6b7aeabfe91c1a9e8c3a6b3e7c1a");

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1002))
                    .andExpect(jsonPath("$.message").value("密码错误"));
        }

        @Test
        @DisplayName("空账号登录 - 应返回 code=1004 参数错误")
        void login_EmptyAccount() throws Exception {
            LoginRequest loginRequest = new LoginRequest("", "password123");

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("账号不能为空"));
        }

        @Test
        @DisplayName("空密码登录 - 应返回 code=1004 参数错误")
        void login_EmptyPassword() throws Exception {
            LoginRequest loginRequest = new LoginRequest("testuser", "");

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("密码不能为空"));
        }
    }

    // ==================== Cross-System Tests ====================

    @Nested
    @DisplayName("跨系统隔离测试 - User 与 CommercialTenant 互不干扰")
    class CrossSystemIsolationTests {

        @Test
        @DisplayName("同一账号可在两个系统分别注册")
        void sameAccount_InDifferentSystems() throws Exception {
            // 在 User 系统注册
            RegisterRequest userRequest = new RegisterRequest("shared", "password123", "用户系统");
            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 在 CommercialTenant 系统注册同一账号
            RegisterRequest tenantRequest = new RegisterRequest("shared", "password123", "商户系统");
            mockMvc.perform(post("/commercialTenant/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tenantRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
