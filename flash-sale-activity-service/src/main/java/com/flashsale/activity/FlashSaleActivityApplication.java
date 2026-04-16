package com.flashsale.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashSaleActivityApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleActivityApplication.class, args);
    }
}
