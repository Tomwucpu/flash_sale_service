package com.flashsale.order.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTaskConfigurationTest {

    @Test
    void defaultExportDirectoryIsOutsideProjectWorkspace() throws Exception {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader()
                .load("order-service", new ClassPathResource("application.yml"));

        Object exportDirectory = propertySources.stream()
                .map(propertySource -> propertySource.getProperty("flash-sale.export.directory"))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);

        assertThat(exportDirectory)
                .isEqualTo("${FLASH_SALE_EXPORT_DIR:${user.home}/flash-sale-exports}");
    }
}
