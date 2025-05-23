package com.example.pricingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI documentation
     *
     * @return the OpenAPI configuration
     */
    @Bean
    public OpenAPI pricingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pricing Service API")
                        .description("RESTful API for retrieving pricing information for retail products")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}