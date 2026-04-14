package com.flashsale.common.security.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserContextHolderTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void setAndGetReturnSameContext() {
        UserContext expected = new UserContext(1001L, "operator", "ADMIN");

        UserContextHolder.set(expected);

        assertEquals(expected, UserContextHolder.get());
    }

    @Test
    void clearRemovesThreadLocalContext() {
        UserContextHolder.set(new UserContext(2002L, "buyer", "USER"));

        UserContextHolder.clear();

        assertNull(UserContextHolder.get());
    }
}
