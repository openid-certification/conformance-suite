package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-invalid-iss-in-entity-configuration",
	displayName = "openid-federation-automatic-client-registration-invalid-iss-in-entity-configuration",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP. " +
		"When the test has been started, it is expecting a request to its .well-known/openid-federation " +
		"endpoint. The returned entity configuration has an invalid issuer which the OP must reject and " +
		"therefore not attempt to continue the flow.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidIssInEntityConfigurationTest extends OpenIDFederationAutomaticClientRegistrationTest {

	private boolean startingShutdown = false;
	private boolean startingSkipped = false;

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		startWaitingForSkip();
		setStatus(Status.WAITING);
	}

	@Override
	protected Object entityConfigurationResponse() {
		if (startingShutdown) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("The test has already run to completion.");
		}

		String iss = env.getString("server", "iss");
		env.putString("server", "iss", iss + "/1");

		env.mapKey("entity_configuration_claims", "server");
		env.mapKey("entity_configuration_claims_jwks", "rp_ec_jwks");
		startWaitingForTimeout();
		Object entityConfigurationResponse = NonBlocking.entityConfigurationResponse(env, getId());
		env.unmapKey("entity_configuration_claims");
		env.unmapKey("entity_configuration_claims_jwks");

		env.putString("server", "iss", iss);

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
