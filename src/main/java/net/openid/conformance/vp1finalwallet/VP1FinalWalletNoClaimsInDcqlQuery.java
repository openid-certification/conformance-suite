package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.condition.client.RemoveClaimsFromDcqlQuery;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-no-claims-in-dcql-query",
	displayName = "OID4VP-1.0-FINAL: DCQL query without claims array",
	summary = "Sends a DCQL query that requests a credential by type (vct) but without specifying "
		+ "individual claims. The wallet should still return a matching credential, but because no "
		+ "selectively-disclosable claims were requested it should disclose only mandatory claims and "
		+ "not include SD-JWT disclosures.",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletNoClaimsInDcqlQuery extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.insertAfter(ExtractDCQLQueryFromClientConfiguration.class,
			condition(RemoveClaimsFromDcqlQuery.class));

		return steps;
	}
}
