package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "openid-federation-resolve",
	displayName = "OpenID Federation: resolve",
	summary = "This test verifies the behavior of the federation_resolve_endpoint provided in the entity's federation_entity metadata. " +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.anchor",
		"federation.trust_anchor_jwks",
	}
)
public class OpenIDFederationResolveTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		final String listEndpoint = env.getString("federation_list_endpoint");
		final String resolveEndpoint = env.getString("federation_resolve_endpoint");
		if (listEndpoint == null || resolveEndpoint == null) {
			fireTestSkipped("Entity metadata does not contain a federation_list_endpoint/federation_resolve_endpoint.");
		}

		List<String> subordinates = getSubordinates(listEndpoint);
		validateResolveEndpoint(subordinates);

		fireTestFinished();
	}

	protected void validateResolveEndpoint(List<String> subordinates) {
		env.putString("entity_statement_url", env.getString("federation_resolve_endpoint"));
		env.putString("expected_sub", subordinates.get(0));

		callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(AppendAnchorToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		eventLog.startBlock(String.format("Fetching resolved metadata from %s", env.getString("entity_statement_url")));

		callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		env.mapKey("endpoint_response", "entity_statement_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(EnsureContentTypeEntityStatementJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.unmapKey("endpoint_response");

		/*
		callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		call(sequence(ValidateEntityStatementBasicClaimsSequence.class));

		callAndContinueOnFailure(ExtractJWKsFromPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		call(sequence(ValidateEntityStatementSignatureSequence.class));

		callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		*/

		eventLog.endBlock();
	}
}
