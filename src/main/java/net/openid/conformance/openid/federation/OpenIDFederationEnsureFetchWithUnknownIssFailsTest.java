package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PublishTestModule(
	testName = "openid-federation-ensure-fetch-with-unknown-iss-fails",
	displayName = "OpenID Federation: Ensure fetch with unknown iss fails",
	summary = "This test verifies the behavior of the federation_fetch_endpoint provided in the entity's federation_entity metadata. " +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEnsureFetchWithUnknownIssFailsTest extends AbstractOpenIDFederationTest {

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
		Optional<String> firstSubordinate = subordinates.stream().findFirst();
		if (firstSubordinate.isPresent()) {
			validateFetchEndpointForIssAndSubAndExpectError(String.format("https://%s.com", UUID.randomUUID()), firstSubordinate.get());
		}

		fireTestFinished();
	}

	protected void validateFetchEndpointForIssAndSubAndExpectError(String iss, String sub) {
		final String fetchEndpoint = env.getString("federation_fetch_endpoint");
		env.putString("entity_statement_url", fetchEndpoint);
		env.putString("expected_iss", iss);
		env.putString("expected_sub", sub);
		callAndContinueOnFailure(AppendIssToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("entity_statement_url")));
		callAndContinueOnFailure(GetEntityStatementAndExpectError.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
	}
}
