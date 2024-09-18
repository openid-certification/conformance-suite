package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;
import java.util.UUID;

@PublishTestModule(
	testName = "openid-federation-ensure-fetch-with-missing-iss-fails",
	displayName = "OpenID Federation: Ensure fetch with missing iss fails",
	summary = "This test verifies the behavior of the federation_fetch_endpoint provided in the entity's federation_entity metadata. " +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEnsureFetchWithMissingIssFailsTest extends AbstractOpenIDFederationEnsureFetchFailsTest {

	@Override
	protected void prepareFetchRequest(List<String> subordinates) {
		String sub = subordinates.stream().findFirst().get();

		final String fetchEndpoint = env.getString("federation_fetch_endpoint");
		env.putString("entity_statement_url", fetchEndpoint);
		env.putString("expected_sub", sub);
		callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
	}

}
