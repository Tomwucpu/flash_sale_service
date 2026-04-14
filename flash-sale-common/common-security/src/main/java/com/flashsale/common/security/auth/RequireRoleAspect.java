package com.flashsale.common.security.auth;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.context.UserContextHolder;
import com.flashsale.common.security.exception.ForbiddenException;
import com.flashsale.common.security.exception.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
public class RequireRoleAspect {

    @Around("@annotation(requireRole)")
    public Object around(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0 || isBlank(userContext.role())) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }

        Set<String> allowedRoles = Arrays.stream(requireRole.value())
                .collect(Collectors.toSet());
        if (!allowedRoles.contains(userContext.role())) {
            throw new ForbiddenException("当前用户无权访问该资源");
        }

        return joinPoint.proceed();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
