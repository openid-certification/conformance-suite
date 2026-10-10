package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.DisallowAccessTokenInQuery;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-on-access-token-in-query",
	displayName = "OID4VCI 1.0: Issuer fail on access token in query",
	summary = """
	This test case validates the standard credential issuance flow using an emulated wallet, as defined in the OpenID
	for Verifiable Credential Issuance (OpenID4VCI) specification. It begins by retrieving metadata from both the
	Credential Issuer and the OAuth 2.0 Authorization Server. An access token is obtained: with the authorization code grant through a Pushed Authorization Request (PAR), or with the pre-authorized code grant directly from the token endpoint.

	If the credential configuration requires key proofs, the test fetches a c_nonce from the Nonce Endpoint (if the issuer advertises one) and includes it in the proof. It then obtains a credential from the Credential Endpoint as normal, then repeats the request with the access token in a query parameter instead of the Authorization header.

	As FAPI SP 2.0 Final prohibits the use of access tokens in query parameter the request should fail.
	""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnRequestWithAccessTokenInQuery extends AbstractVCIIssuerTestModule {

	@Override
	protected void requestProtectedResource() {
		super.requestProtectedResource();
		eventLog.startBlock("Attempt to credential resource endpoint with access token in query parameter");
		updateResourceRequest();
		callAndStopOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.4-2");
		eventLog.endBlock();
	}
}
