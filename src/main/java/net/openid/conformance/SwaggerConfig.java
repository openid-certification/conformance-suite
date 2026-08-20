package net.openid.conformance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import net.openid.conformance.security.WebSecurityResourceServerConfig;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_AUTH_SCHEME = "bearerAuth";

	public static final String TAG_TEST_PLANS = "Test Plans";
	public static final String TAG_TEST_RUNNER = "Test Runner";
	public static final String TAG_TEST_INFORMATION = "Test Information";
	public static final String TAG_TEST_LOGS = "Test Logs";
	public static final String TAG_API_TOKENS = "API Tokens";
	public static final String TAG_USER_AND_PREFERENCES = "User & Preferences";
	public static final String TAG_SERVER = "Server";

	static {
		// Request/response bodies typed as Gson classes are opaque JSON, not the Gson object
		// model — without this springdoc introspects JsonObject etc. and emits schemas full of
		// asDouble/asJsonArray properties. Only JsonObject currently appears in handler
		// signatures, but any Gson type would leak its object model the same way, so the whole
		// family is registered (replaceWithClass is exact-class-keyed and does not cover subtypes).
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
				// Tag descriptions live here (not in the controllers' @Tag annotations) so each
				// tag has one source of truth; the list order is the display order in swagger-ui.
				.tags(List.of(
					new Tag().name(TAG_TEST_PLANS).description("Create and manage test plan instances, and discover the available test plans"),
					new Tag().name(TAG_TEST_RUNNER).description("Create, start, monitor, and cancel running test module instances"),
					new Tag().name(TAG_TEST_INFORMATION).description("Per-test metadata: status, results, publishing and sharing"),
					new Tag().name(TAG_TEST_LOGS).description("Test log entries, exports, and screenshot/image attachments"),
					new Tag().name(TAG_API_TOKENS).description("Manage API tokens for authenticating to this API"),
					new Tag().name(TAG_USER_AND_PREFERENCES).description("The current user and their saved preferences"),
					new Tag().name(TAG_SERVER).description("Information about this conformance suite deployment")))
				.info(new Info().title("OpenID Conformance Suite REST APIs")
						.description("The REST APIs for driving the OpenID Conformance Suite."
							+ " <h3>Authentication</h3>"
							+ " To call the APIs supply a bearer token, which can be either:"
							+ " <ul>"
							+ "   <li>an API token obtained from the <a href='/tokens.html'>token management page</a> (full access, scoped to the token's owner), or</li>"
							+ "   <li>a share-link JWT issued by POST /api/info/{testId}/share or POST /api/plan/{id}/share (read-only access scoped to the shared plan and its tests).</li>"
							+ " </ul>"
							+ " <h3>Typical workflow</h3>"
							+ " Most users want to run a test plan and collect its results:"
							+ " <ol>"
							+ "   <li>Find the plan to run, its variants and its configuration fields: GET /api/plan/available."
							+ " (Tip: after creating the plan once in the <a href='/schedule-test.html'>web UI</a>,"
							+ " GET /api/lastconfig returns the plan name, variant selection and configuration JSON needed for the next step.)</li>"
							+ "   <li>Create a plan instance: POST /api/plan?planName=...&amp;variant=... with the configuration JSON."
							+ " The response contains the plan id and the list of modules to run.</li>"
							+ "   <li>For each module, create a test instance with POST /api/runner?test={testModule}&amp;plan={planId},"
							+ " then wait for it to finish with GET /api/runner/{id}/wait-state (or by polling GET /api/info/{id})."
							+ " A test in the WAITING state needs interaction: GET /api/runner/{id} lists any front-channel URLs to visit.</li>"
							+ "   <li>Read the outcome from GET /api/info/{id} (status and result)"
							+ " and the detailed log from GET /api/log/{id}.</li>"
							+ "   <li>Download the plan's results with GET /api/plan/exporthtml/{id},"
							+ " or prepare a certification package with POST /api/plan/{id}/certificationpackage.</li>"
							+ " </ol>"
							+ " The <a href='https://gitlab.com/openid/conformance-suite/-/blob/master/scripts/run-test-plan.py'>run-test-plan.py script</a>"
							+ " implements this whole flow (it is what our own CI uses) and adds functionality like suppressing known failures;"
							+ " the tutorial linked below shows how to use it."
							+ " <p>The <a href='/v3/api-docs'>raw OpenAPI document</a> behind this page can be fed to an OpenAPI client generator.</p>")
						.version(version))
				.externalDocs(new ExternalDocumentation()
						.description("Step-by-step tutorial: automated conformance testing and certification package creation")
						.url("https://gitlab.com/openid/conformance-suite-automated-testing-tutorial"))
				.components(new Components().addSecuritySchemes(BEARER_AUTH_SCHEME,
					new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.description("Either an API token from <a href='/tokens.html'>the token management page</a>"
							+ " or a share-link JWT from a <code>/share</code> endpoint.")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
	}

	/**
	 * Document the authentication-layer 401 on every operation, merged into an operation's own
	 * 401 where one is declared (POST /api/runner uses 401 for an immutable plan). Operations
	 * that clear the document-wide security requirement with an empty @SecurityRequirements
	 * (permitAll endpoints such as /jwks) are skipped.
	 */
	@Bean
	public GlobalOperationCustomizer documentAuthenticationResponses() {
		String authDescription = "Missing or invalid bearer token / login session";
		return (operation, handlerMethod) -> {
			if (declaresNoSecurity(handlerMethod)) {
				return operation;
			}
			ApiResponse existing = operation.getResponses().get("401");
			if (existing != null) {
				existing.setDescription(existing.getDescription() + " / " + authDescription);
			} else {
				operation.getResponses().addApiResponse("401", new ApiResponse().description(authDescription));
			}
			return operation;
		};
	}

	private static boolean declaresNoSecurity(HandlerMethod handlerMethod) {
		SecurityRequirements requirements = handlerMethod.getMethodAnnotation(SecurityRequirements.class);
		if (requirements == null) {
			requirements = handlerMethod.getBeanType().getAnnotation(SecurityRequirements.class);
		}
		return requirements != null && requirements.value().length == 0;
	}

	/**
	 * The GET operations behind the security configuration's public matcher accept anonymous
	 * requests when ?public=true requests published data: relax their security requirement to
	 * bearer-or-nothing and say so on their 401. Driven by the same path patterns the security
	 * configuration uses, so the two cannot drift.
	 */
	@Bean
	public OpenApiCustomizer documentAnonymousPublicAccess() {
		PathPatternParser parser = new PathPatternParser();
		List<PathPattern> publicPatterns = WebSecurityResourceServerConfig.PUBLIC_GET_PATHS.stream()
			.map(parser::parse)
			.toList();
		return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
			Operation get = pathItem.getGet();
			if (get == null) {
				return;
			}
			PathContainer pathContainer = PathContainer.parsePath(path);
			if (publicPatterns.stream().noneMatch(pattern -> pattern.matches(pathContainer))) {
				return;
			}
			// [{bearerAuth}, {}]: authentication is optional — anonymous callers may request published data
			get.setSecurity(List.of(
				new SecurityRequirement().addList(BEARER_AUTH_SCHEME),
				new SecurityRequirement()));
			ApiResponse unauthorized = get.getResponses() == null ? null : get.getResponses().get("401");
			if (unauthorized != null) {
				unauthorized.setDescription(unauthorized.getDescription()
					+ "; anonymous requests are accepted when public=true requests published data");
			}
		});
	}

}
