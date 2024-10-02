package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "openid-federation-ensure-fetch-with-missing-sub-fails",
	displayName = "OpenID Federation: Ensure fetch with missing sub fails",
	summary = "This test verifies the behavior of the federation_fetch_endpoint provided in the entity's federation_entity metadata. " +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEnsureFetchWithMissingSubFailsTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		final String listEndpoint = env.getString("federation_list_endpoint");
		final String fetchEndpoint = env.getString("federation_fetch_endpoint");
		if (listEndpoint == null || fetchEndpoint == null) {
			fireTestSkipped("Entity metadata does not contain a federation_list_endpoint/federation_fetch_endpoint.");
		}

		List<String> subordinates = getSubordinates(listEndpoint);
		if (!subordinates.isEmpty()) {

			env.putString("entity_statement_url", fetchEndpoint);
			env.removeNativeValue("expected_sub");
			callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

			eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("entity_statement_url")));
			callAndContinueOnFailure(CallFederationEndpointAndExpectError.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			eventLog.endBlock();
		}

		fireTestFinished();
	}

}
