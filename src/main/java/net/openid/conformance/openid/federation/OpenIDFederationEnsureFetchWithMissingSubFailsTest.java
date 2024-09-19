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
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEnsureFetchWithMissingSubFailsTest extends AbstractOpenIDFederationEnsureFetchFailsTest {

	@Override
	protected void prepareFetchRequest(List<String> subordinates) {
		final String fetchEndpoint = env.getString("federation_fetch_endpoint");
		env.putString("entity_statement_url", fetchEndpoint);
		env.removeNativeValue("expected_sub");
		callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
	}

}
