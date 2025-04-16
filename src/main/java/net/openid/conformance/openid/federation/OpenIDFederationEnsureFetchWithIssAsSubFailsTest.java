package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-ensure-fetch-with-iss-as-sub-fails",
	displayName = "OpenID Federation: Ensure fetch with iss as sub fails",
	summary = "This test verifies that the Fetch endpoint responds with JSON and an error when " +
		"the sub parameter references the Entity Identifier of the Issuing Entity. " +
		"The test is isolated to the provided entity and will not proceed to its superiors nor subordinates.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEnsureFetchWithIssAsSubFailsTest extends AbstractOpenIDFederationTest {

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
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		String entityIdentifier = OIDFJSON.getString(env.getElementFromObject("config", "federation.entity_identifier"));
		env.putString("expected_sub", entityIdentifier);
		callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");

		eventLog.startBlock(String.format("Retrieving subordinate statement from %s", env.getString("federation_endpoint_url")));
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.2");
		validateFetchErrorResponse();
		eventLog.endBlock();

		fireTestFinished();
	}

}
