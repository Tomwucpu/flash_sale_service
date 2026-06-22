package com.flashsale.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.user.FlashSaleUserApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlashSaleUserApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublisherApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCanSubmitPublisherApplication() throws Exception {
        JsonNode user = registerUser(uniqueUsername("applicant"));

        mockMvc.perform(post("/api/users/publisher-application")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "我想成为发布者来管理活动"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reason").value("我想成为发布者来管理活动"));
    }

    @Test
    void duplicateApplicationIsRejected() throws Exception {
        JsonNode user = registerUser(uniqueUsername("dup"));

        submitApplication(user, "第一次申请");

        mockMvc.perform(post("/api/users/publisher-application")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "第二次申请"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void userCanViewOwnApplication() throws Exception {
        JsonNode user = registerUser(uniqueUsername("viewmy"));
        submitApplication(user, "查看自己的申请");

        mockMvc.perform(get("/api/users/publisher-application/me")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reason").value("查看自己的申请"));
    }

    @Test
    void adminCanApproveApplication() throws Exception {
        JsonNode user = registerUser(uniqueUsername("approve"));
        JsonNode application = submitApplication(user, "请批准我");

        mockMvc.perform(put("/api/admin/users/publisher-applications/{id}/approve", application.get("id").asLong())
                        .header(UserContext.USER_ID_HEADER, 1L)
                        .header(UserContext.USERNAME_HEADER, "admin")
                        .header(UserContext.ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewNote": "审核通过"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewNote").value("审核通过"));

        // 验证用户角色已变更
        mockMvc.perform(get("/api/users/me")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "PUBLISHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("PUBLISHER"));
    }

    @Test
    void adminCanRejectApplication() throws Exception {
        JsonNode user = registerUser(uniqueUsername("reject"));
        JsonNode application = submitApplication(user, "请审核");

        mockMvc.perform(put("/api/admin/users/publisher-applications/{id}/reject", application.get("id").asLong())
                        .header(UserContext.USER_ID_HEADER, 1L)
                        .header(UserContext.USERNAME_HEADER, "admin")
                        .header(UserContext.ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewNote": "理由不充分"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewNote").value("理由不充分"));

        // 验证用户角色未变更
        mockMvc.perform(get("/api/users/me")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void adminCanListApplications() throws Exception {
        JsonNode user = registerUser(uniqueUsername("listapp"));
        submitApplication(user, "列出申请");

        mockMvc.perform(get("/api/admin/users/publisher-applications")
                        .header(UserContext.USER_ID_HEADER, 1L)
                        .header(UserContext.USERNAME_HEADER, "admin")
                        .header(UserContext.ROLE_HEADER, "ADMIN")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void rejectedUserCanReapply() throws Exception {
        JsonNode user = registerUser(uniqueUsername("reapply"));
        JsonNode application = submitApplication(user, "第一次申请");

        // 拒绝
        mockMvc.perform(put("/api/admin/users/publisher-applications/{id}/reject", application.get("id").asLong())
                        .header(UserContext.USER_ID_HEADER, 1L)
                        .header(UserContext.USERNAME_HEADER, "admin")
                        .header(UserContext.ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewNote": "理由不充分"
                                }
                                """))
                .andExpect(status().isOk());

        // 重新申请
        mockMvc.perform(post("/api/users/publisher-application")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "补充理由后重新申请"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private JsonNode registerUser(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "FlashSale@123",
                                  "nickname": "测试用户"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode submitApplication(JsonNode user, String reason) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/publisher-application")
                        .header(UserContext.USER_ID_HEADER, user.get("id").asLong())
                        .header(UserContext.USERNAME_HEADER, user.get("username").asText())
                        .header(UserContext.ROLE_HEADER, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "%s"
                                }
                                """.formatted(reason)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
