package net.openid.conformance;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_AUTH_SCHEME = "bearerAuth";

	@Value("${fintechlabs.version}")
	private String version;

	@Bean
	public OpenAPI conformanceOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("OpenID Conformance Suite REST APIs")
						.description("This page lists the REST APIs for the OpenID Conformance suite."
							+ " To call APIs you must supply a bearer token, which can be either:"
							+ " <ul>"
							+ "   <li>an API token obtained from the <a href='/tokens.html'>token management page</a> (full access, scoped to the token's owner), or</li>"
							+ "   <li>a share-link JWT issued by <code>POST /api/info/{testId}/share</code> or <code>POST /api/plan/{id}/share</code> (read-only access scoped to the shared plan and its tests).</li>"
							+ " </ul>"
							+ " There is a <a href='https://gitlab.com/openid/conformance-suite/-/blob/master/scripts/run-test-plan.py'>python script that drives the API</a> available, which is used in our own CI and provides functionality like allowing known failures to be suppressed.")
						.version(version)
						.license(new License().name("MIT License").url("https://gitlab.com/openid/conformance-suite/-/blob/master/LICENSE.txt")))
				.externalDocs(new ExternalDocumentation()
						.description("OpenID Conformance Wiki Documentation")
						.url("https://gitlab.com/openid/conformance-suite/-/wikis/home"))
				.components(new Components().addSecuritySchemes(BEARER_AUTH_SCHEME,
					new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.description("Either an API token from <a href='/tokens.html'>the token management page</a>"
							+ " or a share-link JWT from a <code>/share</code> endpoint.")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
	}

}
