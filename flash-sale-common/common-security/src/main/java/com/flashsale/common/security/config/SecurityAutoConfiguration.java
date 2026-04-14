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
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(prefix = "flash-sale.security", name = "jwt-secret")
    JwtTokenService jwtTokenService(JwtProperties jwtProperties, Clock clock) {
        return new JwtTokenService(jwtProperties, clock);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    RequireRoleAspect requireRoleAspect() {
        return new RequireRoleAspect();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new UserContextFilter());
        registrationBean.setOrder(Integer.MIN_VALUE);
        return registrationBean;
    }
}
