package com.flashsale.common.security.config;

import com.flashsale.common.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SecurityAutoConfiguration.class
            ));

    @Test
    void contextStartsWithoutJwtTokenServiceWhenSecretMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(JwtTokenService.class);
        });
    }

    @Test
    void contextCreatesJwtTokenServiceWhenSecretProvided() {
        contextRunner
                .withPropertyValues(
                        "flash-sale.security.jwt-secret=0123456789abcdef0123456789abcdef",
                        "flash-sale.security.access-token-ttl=2h"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtTokenService.class);
                });
    }
}
