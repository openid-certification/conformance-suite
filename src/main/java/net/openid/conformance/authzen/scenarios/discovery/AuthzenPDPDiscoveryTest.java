package net.openid.conformance.authzen.scenarios.discovery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.authzen.AbstractAuthzenPDPMetadataTest;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiEndpointPathContainsV1;
import net.openid.conformance.authzen.condition.ValidatePDPIdentifier;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-discovery",
	displayName = "AuthZEN PDP Metadata - Section 9: Discovery",
	summary = "Fetches the PDP metadata from the well-known URL derived from the PDP Identifier (Section 9.2.1) and validates it (certification scenario 9). No API endpoint is called. "
		+ "The metadata endpoint MUST be available: an HTTP 404, a redirect or an empty body fails the test. "
		+ "The response MUST be HTTP 200 with Content-Type application/json and a JSON object body, with no parameter whose value is null or empty (Section 9.2.2). "
		+ "policy_decision_point MUST equal the PDP Identifier (Section 9.2.3); access_evaluation_endpoint MUST be present and every endpoint MUST be an HTTPS URL; capabilities, if present, MUST be an array of strings. "
		+ "signed_metadata, if present, MUST be a JWS with a registered alg other than none whose signature verifies against the PDP JWK Set in the test configuration, "
		+ "with an iss equal to the 'Signed Metadata Issuer' or, when that is empty, the PDP Identifier, an exp not in the past and an nbf not in the future; its values take precedence (Section 9.1.3), and the published values are checked as well. "
		+ "Unknown metadata members, capabilities outside urn:ietf:params:authzen:, implausible signed_metadata time claims, a nested signed_metadata, an advertised endpoint without v1 in its path (Section 4), "
		+ "a published document without access_evaluation_endpoint when signed_metadata supplies one, "
		+ "and a 'PDP JWK Set' or 'Signed Metadata Issuer' configured for metadata without signed_metadata are reported as warnings.",
	profile = "AuthZEN",
	configurationFields = {
		"pdp.policy_decision_point",
		"pdp.jwks",
		"pdp.metadata_issuer"
	}
)
public class AuthzenPDPDiscoveryTest extends AbstractAuthzenPDPMetadataTest {

	/** The Section 9.1.1 endpoint parameters, each naming one API. */
	private static final List<String> ENDPOINT_PARAMETERS = List.of(
		"access_evaluation_endpoint",
		"access_evaluations_endpoint",
		"search_subject_endpoint",
		"search_resource_endpoint",
		"search_action_endpoint");

	/** The endpoint URLs whose path has been checked for v1. */
	private final Set<String> checkedEndpointPaths = new LinkedHashSet<>();

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
		checkAdvertisedEndpointPaths();
		fireTestFinished();
	}

	/**
	 * Section 4: "Endpoints for version 1.0 SHOULD include v1 in the endpoint identifier". The API
	 * modules check the endpoint they call; this module calls none, so it checks every endpoint
	 * the metadata advertises. A PEP that ignores signed metadata (Section 9.1.3) uses the
	 * published values, so when signed_metadata is present they are checked here, before the
	 * signed values replace them, and the signed values are checked after them. Each endpoint URL
	 * is checked once, so a signed value that repeats a published one is not reported twice.
	 */
	@Override
	protected void checkPublishedEndpointsAndCapabilities() {
		super.checkPublishedEndpointsAndCapabilities();
		checkEndpointPaths(uncheckedEndpoints("pdp"));
	}

	/**
	 * Checks the endpoints in effect: the published metadata, or, when signed_metadata is present,
	 * the signed values, the published ones having been checked before they were replaced.
	 */
	private void checkAdvertisedEndpointPaths() {
		List<String> endpoints = uncheckedEndpoints(
			env.containsObject("pdp_signed_metadata_values") ? "pdp_signed_metadata_values" : "pdp");
		if (endpoints.isEmpty()) {
			return;
		}
		eventLog.startBlock("Check the advertised endpoint paths");
		try {
			checkEndpointPaths(endpoints);
		} finally {
			eventLog.endBlock();
		}
	}

	private List<String> uncheckedEndpoints(String envKey) {
		Set<String> endpoints = new LinkedHashSet<>();
		for (String parameter : ENDPOINT_PARAMETERS) {
			JsonElement endpoint = env.getElementFromObject(envKey, parameter);
			if (endpoint != null && OIDFJSON.isString(endpoint) && !checkedEndpointPaths.contains(OIDFJSON.getString(endpoint))) {
				endpoints.add(OIDFJSON.getString(endpoint));
			}
		}
		return List.copyOf(endpoints);
	}

	private void checkEndpointPaths(List<String> endpoints) {
		for (String endpoint : endpoints) {
			checkedEndpointPaths.add(endpoint);
			env.putString("authzen_api_endpoint", endpoint);
			callAndContinueOnFailure(EnsureAuthzenApiEndpointPathContainsV1.class, ConditionResult.WARNING, "AUTHZEN-4");
		}
	}

	@Override
	protected void processCallback() {
		// The module receives no callbacks
	}
}
