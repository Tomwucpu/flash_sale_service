package com.flashsale.common.security.config;

import com.flashsale.common.security.auth.RequireRoleAspect;
import com.flashsale.common.security.jwt.JwtProperties;
import com.flashsale.common.security.jwt.JwtTokenService;
import com.flashsale.common.security.web.UserContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 通用安全组件自动配置。
 * <p>
 * 负责注册 JWT、密码加密、角色鉴权切面以及用户上下文过滤器等基础 Bean。
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

    /**
     * 提供统一系统时钟，便于时间相关逻辑测试与替换。
     */
    @Bean
    Clock clock(@Value("${flash-sale.time-zone:Asia/Shanghai}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }

    /**
     * 在配置了 JWT 密钥时注册令牌服务。
     */
    @Bean
    @ConditionalOnProperty(prefix = "flash-sale.security", name = "jwt-secret")
    JwtTokenService jwtTokenService(JwtProperties jwtProperties, Clock clock) {
        return new JwtTokenService(jwtProperties, clock);
    }

    /**
     * 注册密码加密器（BCrypt）。
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 注册基于注解的角色鉴权切面。
     */
    @Bean
    RequireRoleAspect requireRoleAspect() {
        return new RequireRoleAspect();
    }

    /**
     * 在 Servlet Web 场景下注册用户上下文过滤器。
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new UserContextFilter());
        // 尽可能优先执行，确保后续链路可读取用户上下文
        registrationBean.setOrder(Integer.MIN_VALUE);
        return registrationBean;
    }
}
