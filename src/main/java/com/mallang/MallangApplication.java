package com.mallang;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Mallang API"))
@SpringBootApplication
public class MallangApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallangApplication.class, args);
    }

}
