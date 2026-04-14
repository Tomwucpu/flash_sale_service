package com.flashsale.common.security.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserContextTest {

    @Test
    void fromHeadersParsesGatewayHeaders() {
        UserContext context = UserContext.fromHeaders(Map.of(
                UserContext.USER_ID_HEADER, "1001",
                UserContext.ROLE_HEADER, "ADMIN",
                UserContext.USERNAME_HEADER, "operator"
        ));

        assertEquals(1001L, context.userId());
        assertEquals("ADMIN", context.role());
        assertEquals("operator", context.username());
    }

    @Test
    void toHeadersKeepsCanonicalHeaderNames() {
        UserContext context = new UserContext(2002L, "buyer", "USER");

        assertEquals(Map.of(
                UserContext.USER_ID_HEADER, "2002",
                UserContext.ROLE_HEADER, "USER",
                UserContext.USERNAME_HEADER, "buyer"
        ), context.toHeaders());
    }
}
