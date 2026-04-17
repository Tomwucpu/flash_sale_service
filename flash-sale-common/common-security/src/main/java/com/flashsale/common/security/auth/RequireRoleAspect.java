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

/**
 * 基于 {@link RequireRole} 注解的角色鉴权切面。
 */
@Aspect
public class RequireRoleAspect {

    /**
     * 在目标方法执行前进行登录态与角色校验。
     *
     * @param joinPoint 切点上下文
     * @param requireRole 方法上声明的角色要求
     * @return 目标方法返回值
     * @throws Throwable 透传目标方法执行异常
     */
    @Around("@annotation(requireRole)")
    public Object around(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        UserContext userContext = UserContextHolder.get();
        if (userContext == null || userContext.userId() == null || userContext.userId() <= 0 || isBlank(userContext.role())) {
            throw new UnauthorizedException("未登录或登录状态已失效");
        }

        // 从注解中提取允许访问的角色集合
        Set<String> allowedRoles = Arrays.stream(requireRole.value())
                .collect(Collectors.toSet());
        if (!allowedRoles.contains(userContext.role())) {
            throw new ForbiddenException("当前用户无权访问该资源");
        }

        // 校验通过，放行目标方法
        return joinPoint.proceed();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
