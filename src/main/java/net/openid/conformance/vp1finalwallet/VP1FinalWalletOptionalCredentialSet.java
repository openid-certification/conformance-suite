package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddOptionalNonMatchingCredentialToDcqlQuery;
import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-optional-credential-set",
	displayName = "OID4VP-1.0-FINAL: DCQL query with optional non-matching credential_set",
	summary = """
		Sends a DCQL query with two credential entries wrapped in credential_sets: the real credential \
		is required, and a second non-matching credential is optional. The wallet should return only the \
		real credential and not fail the request due to the unmatchable optional entry. \
		The DCQL configuration must not already contain credential_sets.""",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletOptionalCredentialSet extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertAfter(ExtractDCQLQueryFromClientConfiguration.class,
			condition(AddOptionalNonMatchingCredentialToDcqlQuery.class));

		return steps;
	}
}
