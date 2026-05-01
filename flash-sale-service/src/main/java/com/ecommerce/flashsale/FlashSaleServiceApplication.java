package com.ecommerce.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ecommerce")
@EnableFeignClients
@EnableScheduling
public class FlashSaleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleServiceApplication.class, args);
    }
}
