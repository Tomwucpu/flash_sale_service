package com.flashsale.user.controller;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.auth.RequireRole;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.user.dto.request.PublisherApplicationRequest;
import com.flashsale.user.dto.response.PublisherApplicationResponse;
import com.flashsale.user.service.PublisherApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/publisher-application")
public class PublisherApplicationController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final PublisherApplicationService applicationService;

    public PublisherApplicationController(PublisherApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @RequireRole({"USER"})
    public ApiResponse<PublisherApplicationResponse> apply(
            @Valid @RequestBody PublisherApplicationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                applicationService.apply(UserContextHolder.get(), request)
        );
    }

    @GetMapping("/me")
    @RequireRole({"USER"})
    public ApiResponse<PublisherApplicationResponse> getMyApplication(
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
                requestId(httpServletRequest),
                applicationService.getMyApplication(UserContextHolder.get())
        );
    }

    private String requestId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getHeader(REQUEST_ID_HEADER);
    }
}
