package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.ExtractDCQLQueryFromClientConfiguration;
import net.openid.conformance.condition.client.RemoveClaimsFromDcqlQuery;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-no-claims-in-dcql-query",
	displayName = "OID4VP-1.0-FINAL: DCQL query without claims array",
	summary = """
		Sends a DCQL query that requests a credential by its type (vct for SD-JWT VC, doctype for \
		mdoc) with no claims array. The wallet must still return a matching credential, but as \
		no selectively disclosable claims were requested it must return only the claims that are \
		mandatory to present: for SD-JWT VC, no disclosures; for mdoc, no data elements.""",
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
