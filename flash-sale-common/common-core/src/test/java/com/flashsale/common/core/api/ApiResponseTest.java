package com.flashsale.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void successFactoryBuildsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("REQ-001", "ok");

        assertEquals("SUCCESS", response.code());
        assertEquals("ok", response.message());
        assertEquals("REQ-001", response.requestId());
        assertEquals("ok", response.data());
    }

    @Test
    void failureFactoryBuildsFailureResponse() {
        ApiResponse<Void> response = ApiResponse.failure("OUT_OF_STOCK", "库存不足", "REQ-002");

        assertEquals("OUT_OF_STOCK", response.code());
        assertEquals("库存不足", response.message());
        assertEquals("REQ-002", response.requestId());
        assertNull(response.data());
    }
}
