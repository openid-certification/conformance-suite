package net.openid.conformance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_AUTH_SCHEME = "bearerAuth";

	static {
		// Request/response bodies typed as Gson classes are opaque JSON, not the Gson object
		// model — without this springdoc introspects JsonObject etc. and emits schemas full of
		// asDouble/asJsonArray properties. replaceWithClass is exact-class-keyed, so each Gson
		// type must be registered individually.
		SpringDocUtils.getConfig()
			.replaceWithClass(JsonElement.class, Object.class)
			.replaceWithClass(JsonObject.class, Object.class)
			.replaceWithClass(JsonArray.class, List.class)
			.replaceWithClass(JsonPrimitive.class, Object.class)
			.replaceWithClass(JsonNull.class, Object.class);
	}

	@Value("${fintechlabs.version}")
	private String version;

	@Bean
	public OpenAPI conformanceOpenAPI() {
		return new OpenAPI()
				// No servers entry: springdoc then generates an absolute server URL from the
				// incoming request, which is what swagger-ui's "Try it out" needs — a relative
				// URL here makes it issue page-relative requests (/swagger-ui/api/...).
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
