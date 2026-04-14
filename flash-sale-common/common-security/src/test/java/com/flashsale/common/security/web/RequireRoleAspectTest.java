package com.flashsale.common.security.web;

import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.auth.RequireRoleAspect;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.ForbiddenException;
import com.flashsale.common.security.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = RequireRoleAspectTest.TestConfig.class)
class RequireRoleAspectTest {

    @Autowired
    private ProtectedAction protectedAction;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void invokeRejectsAnonymousUser() {
        assertThrows(UnauthorizedException.class, () -> protectedAction.adminOnly());
    }

    @Test
    void invokeRejectsMismatchedRole() {
        UserContextHolder.set(new UserContext(2001L, "buyer", "USER"));

        assertThrows(ForbiddenException.class, () -> protectedAction.adminOnly());
    }

    @Test
    void invokeAllowsMatchingRole() {
        UserContextHolder.set(new UserContext(3001L, "publisher", "PUBLISHER"));

        assertDoesNotThrow(() -> assertEquals("ok", protectedAction.adminOrPublisher()));
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        RequireRoleAspect requireRoleAspect() {
            return new RequireRoleAspect();
        }

        @Bean
        ProtectedAction protectedAction() {
            return new ProtectedAction();
        }
    }

    static class ProtectedAction {

        @RequireRole("ADMIN")
        String adminOnly() {
            return "ok";
        }

        @RequireRole({"ADMIN", "PUBLISHER"})
        String adminOrPublisher() {
            return "ok";
        }
    }
}
