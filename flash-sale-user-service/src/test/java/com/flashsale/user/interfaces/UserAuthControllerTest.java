package com.flashsale.user.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.user.FlashSaleUserApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSaleUserApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerCreatesEnabledUserWithDefaultRoleAndEncryptedPassword() throws Exception {
        String username = uniqueUsername("buyer");
        String password = "FlashSale@123";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "nickname": "抢购用户",
                                  "phone": "13800000001"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        String storedHash = jdbcTemplate.queryForObject(
                "select password_hash from user where username = ?",
                String.class,
                username
        );

        assertNotNull(storedHash);
        assertNotEquals(password, storedHash);
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        String username = uniqueUsername("repeat");

        register(username, "FlashSale@123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "FlashSale@123",
                                  "nickname": "重复用户"
                                }
                                """.formatted(username)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void loginReturnsJwtWhenCredentialsMatch() throws Exception {
        String username = uniqueUsername("login");
        String password = "FlashSale@123";
        register(username, password);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void meReturnsCurrentUserUsingCanonicalHeaders() throws Exception {
        String username = uniqueUsername("me");
        JsonNode user = register(username, "FlashSale@123");

        mockMvc.perform(get("/api/users/me")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, username)
                        .header(UserContext.ROLE_HEADER, "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(user.get("id").asLong()))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void getUserByIdRejectsPlainUserRole() throws Exception {
        JsonNode user = register(uniqueUsername("denied"), "FlashSale@123");

        mockMvc.perform(get("/api/users/{userId}", user.get("id").asLong())
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getUserByIdAllowsAdminRole() throws Exception {
        JsonNode user = register(uniqueUsername("query"), "FlashSale@123");

        mockMvc.perform(get("/api/users/{userId}", user.get("id").asLong())
                        .header(UserContext.USER_ID_HEADER, 9999L)
                        .header(UserContext.USERNAME_HEADER, "admin")
                        .header(UserContext.ROLE_HEADER, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(user.get("id").asLong()))
                .andExpect(jsonPath("$.data.username").value(user.get("username").asText()));
    }

    private JsonNode register(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "nickname": "测试用户"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
