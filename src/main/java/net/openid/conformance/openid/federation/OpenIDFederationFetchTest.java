package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "openid-federation-fetch",
	displayName = "OpenID Federation: fetch",
	summary = "This test verifies the behavior of the federation_fetch_endpoint provided in the entity's federation_entity metadata. " +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationFetchTest extends AbstractOpenIDFederationTest {

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
		validateFetchEndpoint(subordinates);

		fireTestFinished();
	}

	protected void validateFetchEndpoint(List<String> subordinates) {
		for (String entityIdentifier : subordinates) {
			env.putString("entity_statement_url", env.getString("federation_fetch_endpoint"));
			env.putString("expected_iss", env.getString("primary_entity_statement_iss"));
			env.putString("expected_sub", entityIdentifier);
			callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

			eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("entity_statement_url")));

			callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

			env.mapKey("endpoint_response", "entity_statement_endpoint_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureContentTypeEntityStatementJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("endpoint_response");

			callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			call(sequence(ValidateEntityStatementBasicClaimsSequence.class));

			callAndContinueOnFailure(ExtractJWKsFromPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			call(sequence(ValidateEntityStatementSignatureSequence.class));

			callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

			callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
			callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1");

			callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

			eventLog.endBlock();
		}
	}

}
