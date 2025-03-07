package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.UUID;

@PublishTestModule(
		testName = "openid-federation-ensure-fetch-with-invalid-sub-fails",
		displayName = "OpenID Federation: Ensure fetch with invalid sub fails",
		summary = "This test verifies that the Fetch endpoint responds with JSON and an error when the sub parameter is invalid. " +
				"The test is isolated to the provided entity and will not proceed to its superiors nor subordinates.",
		profile = "OIDFED",
		configurationFields = {
				"federation.entity_identifier",
				"federation.trust_anchor_jwks"
		}
)
public class OpenIDFederationEnsureFetchWithInvalidSubFailsTest extends AbstractOpenIDFederationTest {

	@Override
	public void additionalConfiguration() {
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		final String fetchEndpoint = env.getString("federation_fetch_endpoint");
		if (fetchEndpoint == null) {
			fireTestSkipped("Entity metadata does not contain a federation_fetch_endpoint.");
		}

		env.putString("federation_endpoint_url", fetchEndpoint);
		env.putString("expected_sub", "https://%s".formatted(UUID.randomUUID().toString()));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");

		eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("federation_endpoint_url")));
		callAndStopOnFailure(CallFetchEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
		validateFetchErrorResponse();
		eventLog.endBlock();

		fireTestFinished();
	}

}
