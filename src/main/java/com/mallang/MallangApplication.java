package com.mallang;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "Mallang API"), servers = @Server(url = "/"))
@SecurityScheme(name = "anonymousBearer", type = SecuritySchemeType.HTTP, scheme = "bearer")
@SpringBootApplication
@EnableScheduling
public class MallangApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallangApplication.class, args);
    }

}
