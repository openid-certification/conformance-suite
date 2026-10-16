package net.openid.conformance.authzen.scenarios.discovery;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.AbstractAuthzenPDPMetadataTest;
import net.openid.conformance.authzen.condition.ValidatePDPIdentifier;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-discovery",
	displayName = "AuthZEN PDP Metadata - Section 9: Discovery",
	summary = "Fetches the PDP metadata from the well-known URL derived from the PDP Identifier (Section 9.2.1) and validates it (certification scenario 9). The metadata endpoint MUST be available: an HTTP 404 fails the test. The response MUST be HTTP 200 with Content-Type application/json and a JSON object body; policy_decision_point MUST equal the PDP Identifier; access_evaluation_endpoint MUST be present and every endpoint MUST be an HTTPS URL; capabilities, if present, MUST be an array of strings; signed_metadata, if present, MUST be a JWT with an iss claim whose signature verifies against the PDP JWK Set in the test configuration. No API endpoint is called.",
	profile = "AuthZEN",
	configurationFields = {
		"pdp.policy_decision_point",
		"pdp.jwks",
		"pdp.metadata_issuer"
	}
)
public class AuthzenPDPDiscoveryTest extends AbstractAuthzenPDPMetadataTest {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		if (!storeConfigurationUnlessSkipped(config, baseUrl, externalUrlOverride, baseMtlsUrl)) {
			return;
		}
		callAndStopOnFailure(ValidatePDPIdentifier.class, "AUTHZEN-9.1.1");
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		fetchAndValidateDiscoveryMetadata();
		checkPDPServerConfiguration(true);
		fireTestFinished();
	}

	@Override
	protected void processCallback() {
		// The module receives no callbacks
	}
}
