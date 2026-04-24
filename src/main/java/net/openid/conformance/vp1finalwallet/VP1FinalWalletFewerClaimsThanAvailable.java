package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.condition.client.RemoveLastClaimFromDcqlQuery;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-fewer-claims-than-available",
	displayName = "OID4VP-1.0-FINAL: Request fewer claims than the wallet has available",
	summary = """
		Sends a DCQL query with one fewer claim than configured, testing that the wallet handles \
		a subset of available claims. The test checks data minimization — wallets should ideally only \
		disclose the claims that were requested, not additional ones. \
		The DCQL configuration must contain at least 2 claims.""",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletFewerClaimsThanAvailable extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertAfter(ExtractDCQLQueryFromClientConfiguration.class,
			condition(RemoveLastClaimFromDcqlQuery.class));

		return steps;
	}
}
