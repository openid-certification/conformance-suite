package net.openid.conformance;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${fintechlabs.version}")
    private String version;

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("OpenID Conformance Suite REST APIs")
                        .description("This page lists the REST APIs for the OpenID Conformance suite. You must obtain a token from the token management page to call APIs. There is a python script that drives the API available, which is used in our own CI and provides functionality like allowing known failures to be suppressed.")
                        .version(version)
                        .license(new License().name("MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("OpenID wiki")
                        .url("https://openid.net/certification/"));
    }

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .displayName("Conformance API")
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }
}
