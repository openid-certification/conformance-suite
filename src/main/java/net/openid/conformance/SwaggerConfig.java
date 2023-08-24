package net.openid.conformance;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Value("${fintechlabs.version}")
	private String version;

	@Bean
	public OpenAPI conformanceOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("OpenID Conformance Suite REST APIs")
						.description("This page lists the REST APIs for the OpenID Conformance suite. You must obtain a token from the <a href='/tokens.html'>token management page</a> to call APIs. There is a <a href='https://gitlab.com/openid/conformance-suite/-/blob/master/scripts/run-test-plan.py'>python script that drives the API</a> available, which is used in our own CI and provides functionality like allowing known failures to be suppressed.")
						.version(version)
						.license(new License().name("MIT License").url("https://gitlab.com/openid/conformance-suite/-/blob/master/LICENSE.txt")))
				.externalDocs(new ExternalDocumentation()
						.description("OpenID Conformance Wiki Documentation")
						.url("https://gitlab.com/openid/conformance-suite/-/wikis/home"));
	}

}
