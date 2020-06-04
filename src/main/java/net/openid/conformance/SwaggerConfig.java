package net.openid.conformance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Value("${fintechlabs.version}")
	private String version;

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).select()
			.apis(RequestHandlerSelectors.basePackage("net.openid.conformance"))
			.paths(PathSelectors.regex("/api/.*"))
			.build()
			.apiInfo(apiInfo())
			.useDefaultResponseMessages(false);
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
			.title("OpenID Conformance Suite REST APIs")
			.description("This page lists the REST APIs for the OpenID Conformance suite. You must obtain a token from the <a href='tokens.html'>token management page</a> to call APIs. There is a <a href='https://gitlab.com/openid/conformance-suite/-/blob/master/scripts/run-test-plan.py'>python script that drives the API</a> available, which is used in our own CI and provides functionality like allowing known failures to be suppressed.")
			.version(version)
			.build();
	}

}
