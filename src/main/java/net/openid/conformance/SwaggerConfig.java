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
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import net.openid.conformance.security.JwksEndpoint;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
							+ "   <li>a share-link JWT issued by <code>POST /api/info/{testId}/share</code> or <code>POST /api/plan/{id}/share</code> (read-only access scoped to the shared plan and its tests).</li>"
							+ " </ul>"
							+ " <h3>Typical workflow</h3>"
							+ " Most users want to run a test plan and collect its results:"
							+ " <ol>"
							+ "   <li>Find the plan to run, its variants and its configuration fields:"
							+ " <a href='#/Test%20Plans/listAvailableTestPlans'><code>GET /api/plan/available</code></a>."
							+ " (Tip: after creating the plan once in the <a href='/schedule-test.html'>web UI</a>,"
							+ " <a href='#/User%20&amp;%20Preferences/getLastConfig'><code>GET /api/lastconfig</code></a> returns the"
							+ " plan name, variant selection and configuration JSON needed for the next step.)</li>"
							+ "   <li>Create a plan instance: <a href='#/Test%20Plans/createTestPlan'><code>POST /api/plan?planName=...&amp;variant=...</code></a> with the configuration JSON."
							+ " The response contains the plan <code>id</code> and the list of <code>modules</code> to run.</li>"
							+ "   <li>For each module, create a test instance with <a href='#/Test%20Runner/createTest'><code>POST /api/runner?test={testModule}&amp;plan={planId}</code></a>,"
							+ " then wait for it to finish with <a href='#/Test%20Runner/waitForTestState'><code>GET /api/runner/{id}/wait-state</code></a>"
							+ " (or by polling <a href='#/Test%20Information/getTestInfo'><code>GET /api/info/{id}</code></a>)."
							+ " A test in the <code>WAITING</code> state needs interaction: <a href='#/Test%20Runner/getTestStatus'><code>GET /api/runner/{id}</code></a> lists any front-channel URLs to visit.</li>"
							+ "   <li>Read the outcome from <a href='#/Test%20Information/getTestInfo'><code>GET /api/info/{id}</code></a> (<code>status</code> and <code>result</code>)"
							+ " and the detailed log from <a href='#/Test%20Logs/getTestLog'><code>GET /api/log/{id}</code></a>.</li>"
							+ "   <li>Download the plan's results with <a href='#/Test%20Plans/exportPlanLogsHtml'><code>GET /api/plan/exporthtml/{id}</code></a>,"
							+ " or prepare a certification package with <a href='#/Test%20Plans/prepareCertificationPackage'><code>POST /api/plan/{id}/certificationpackage</code></a>.</li>"
							+ " </ol>"
							+ " The <a href='https://gitlab.com/openid/conformance-suite/-/blob/master/scripts/run-test-plan.py'>run-test-plan.py script</a>"
							+ " implements this whole flow (it is what our own CI uses) and adds functionality like suppressing known failures;"
							+ " the tutorial linked below shows how to use it."
							+ " <p>The raw OpenAPI document behind this page is available at the <code>/v3/api-docs</code> link under the title above,"
							+ " and can be fed to an OpenAPI client generator.</p>")
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
	 * Document the authentication-layer 401 on every operation. Merged into an operation's own
	 * 401 where one is declared (POST /api/runner uses 401 for an immutable plan). /jwks is
	 * excluded: it is permitAll in the security configuration.
	 */
	@Bean
	public GlobalOperationCustomizer documentAuthenticationResponses() {
		String authDescription = "Missing or invalid bearer token / login session"
			+ " (for endpoints with a 'public' parameter, only when not requesting published data)";
		return (operation, handlerMethod) -> {
			if (handlerMethod.getBeanType() == JwksEndpoint.class) {
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

}
