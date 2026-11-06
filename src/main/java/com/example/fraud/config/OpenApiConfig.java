package com.example.fraud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Describes the API in Swagger UI (http://localhost:8080/swagger-ui.html), including the Basic-auth login button. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fraudRuleEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud Rule Engine API(CapiTest)")
                        .version("1.0.0")
                        .description("Evaluates categorised transactions against configurable fraud rules, "
                                + "stores fraud flags and lets analysts review them."))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
