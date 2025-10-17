package net.openid.conformance.openid.federation;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
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
		"endpoint, after which the test is completed. If no such request is received within 30 seconds, " +
		"the test is skipped.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.rp_client_jwks",
		"federation.rp_entity_identifier_host_override",
		"federation_trust_anchor.immediate_subordinates",
		"federation_trust_anchor.trust_anchor_jwks",
		"internal.op_to_rp_mode",
	}
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationExpectEntityConfigurationRequestTest extends AbstractOpenIDFederationAutomaticClientRegistrationTest {

	private boolean startingShutdown = false;
	private boolean startingSkipped = false;

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

		String hostOverride = OIDFJSON.getStringOrNull(config.get("federation").getAsJsonObject().get("op_entity_identifier_host_override"));
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
		startWaitingForSkip();
	}

	@Override
	protected Object entityConfigurationResponse() {
		if (startingShutdown) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("The test has already run to completion.");
		}

		env.mapKey("entity_configuration_claims", "server");
		env.mapKey("entity_configuration_claims_jwks", "client_jwks");
		startWaitingForTimeout();
		Object entityConfigurationResponse = NonBlocking.entityConfigurationResponse(env, getId());
		env.unmapKey("entity_configuration_claims");
		env.unmapKey("entity_configuration_claims_jwks");

		return entityConfigurationResponse;
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

	protected void startWaitingForSkip() {
		if (startingSkipped) {
			return;
		}

		this.startingSkipped = true;
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(30 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				fireTestSkipped("No request to the entity configuration endpoint has been received after 30 seconds.");
			}
			return "done";
		});
	}

}
