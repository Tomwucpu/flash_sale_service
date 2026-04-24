package com.flashsale.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashSaleOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleOrderApplication.class, args);
    }
}
