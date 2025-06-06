package net.openid.conformance.openid.federation;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.openid.federation.client.SignEntityStatementWithClientKeys;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-expect-entity-configuration-request",
	displayName = "openid-federation-automatic-client-registration-expect-entity-configuration-request",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP. " +
		"When the test has been started, it is expecting a request to its .well-known/openid-federation " +
		"endpoint, after which the test is completed.",
	profile = "OIDFED",
	configurationFields = {
		"client.jwks",
		"client.trust_chain",
		"federation.entity_identifier",
		"federation.entity_identifier_host_override",
		"federation.trust_anchor",
		"federation.trust_anchor_jwks",
		"federation.authority_hints",
		"internal.op_to_rp_mode"
	}
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationExpectEntityConfigurationRequestTest extends AbstractOpenIDFederationAutomaticClientRegistrationTest {

	private boolean startingShutdown = false;

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return null;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return null;
	}

	@Override
	protected void verifyTestConditions() {
	}

	@Override
	protected void redirect(HttpMethod method) {
		performRedirect(method.name());
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		String hostOverride = OIDFJSON.getString(config.get("federation").getAsJsonObject().get("entity_identifier_host_override"));
		if (!Strings.isNullOrEmpty(hostOverride)) {
			baseUrl = EntityUtils.replaceHostnameInUrl(baseUrl, hostOverride);
		}

		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		additionalConfiguration();

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		return switch (path) {
			case ".well-known/openid-federation" -> entityConfigurationResponse();
			case "jwks" -> clientJwksResponse();
			default -> null;
		};
	}

	@Override
	protected Object entityConfigurationResponse() {
		if (opToRpMode()) {
			env.mapKey("entity_configuration_claims", "server");
			env.mapKey("entity_configuration_claims_jwks", "client_jwks");
			startWaitingForTimeout();
			return NonBlocking.entityConfigurationResponse(env, getId());
		}

		setStatus(Status.RUNNING);

		env.mapKey("entity_configuration_claims", "server");
		callAndStopOnFailure(SignEntityStatementWithClientKeys.class);
		env.unmapKey("entity_configuration_claims");
		String entityConfiguration = env.getString("signed_entity_statement");

		setStatus(Status.WAITING);
		startWaitingForTimeout();

		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
			.body(entityConfiguration);
	}

	// Allow additional calls to come in for 5 more seconds.
	protected void startWaitingForTimeout() {
		if (startingShutdown) {
			return;
		}

		this.startingShutdown = true;
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(5 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				fireTestFinished();
			}
			return "done";
		});
	}

}
