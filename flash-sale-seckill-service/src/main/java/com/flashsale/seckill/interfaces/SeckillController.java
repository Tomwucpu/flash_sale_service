package com.flashsale.seckill.interfaces;

import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.seckill.application.SeckillAttemptResult;
import com.flashsale.seckill.application.SeckillResultResponse;
import com.flashsale.seckill.application.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/activities/{activityId}/attempt")
    public ApiResponse<?> attempt(@PathVariable Long activityId, HttpServletRequest request) {
        String requestId = requiredRequestId(request);
        SeckillAttemptResult result = seckillService.attempt(activityId, requestId, requiredUserContext());
        return new ApiResponse<>(result.code(), result.message(), requestId, result.data());
    }

    @GetMapping("/results/{activityId}")
    public ApiResponse<SeckillResultResponse> result(@PathVariable Long activityId, HttpServletRequest request) {
        return ApiResponse.success(
                requestId(request),
                seckillService.queryResult(activityId, requiredUserContext())
        );
    }

    private UserContext requiredUserContext() {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0) {
            throw new UnauthorizedException("当前用户未登录");
        }
        return userContext;
    }

    private String requiredRequestId(HttpServletRequest request) {
        String requestId = requestId(request);
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("缺少 X-Request-Id 请求头");
        }
        return requestId;
    }

    private String requestId(HttpServletRequest request) {
        return request.getHeader(REQUEST_ID_HEADER);
    }
}
