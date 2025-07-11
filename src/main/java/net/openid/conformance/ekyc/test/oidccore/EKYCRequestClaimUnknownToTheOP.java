package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.ekyc.condition.client.AddUnknownVerifiedClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	Based on Yes.com test: "Test: Claim that is unknown to the OP. Expected result: Claim is omitted from response."
 */
@PublishTestModule(
	testName = "ekyc-server-unknown-claim-omitted",
	displayName = "eKYC Server Test - Unknown claim omitted from response",
	summary = "This test requests one known claim, selected from the list of claims_in_verified_claims_supported, " +
		"and one random claim name (unknown to the OP) and expects a happy path flow." +
		" The unknown claim must be omitted from responses.",
	profile = "OIDCC",
	configurationFields = {
		"ekyc.unverified_claims_names",
		"ekyc.verified_claims_names",
		"ekyc.request_schemas",
		"ekyc.response_schemas"
	}
)
public class EKYCRequestClaimUnknownToTheOP extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddUnknownVerifiedClaimToAuthorizationEndpointRequest.class);
	}

	//Note: as we don't check if all requested claims were returned by default, no additional response validation is required
}
