package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.ekyc.condition.client.AddEssentialUnknownVerifiedClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	Based on Yes.com test: "Test: Essential Claim that is unknown to the OP. Expected result: Claim is omitted from response"
 */
@PublishTestModule(
	testName = "ekyc-server-unknown-essential-claim-omitted",
	displayName = "eKYC Server Test - Unknown essential claim omitted from response",
	summary = "This test requests one known claim, selected from the list of claims_in_verified_claims_supported, " +
		"and one random claim name (unknown to the OP), marked as essential, and expects a happy path flow." +
		" The unknown claim must be omitted from responses.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCRequestEssentialClaimUnknownToTheOP extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddEssentialUnknownVerifiedClaimToAuthorizationEndpointRequest.class);
	}

	//Note: as we don't check if all requested claims were returned by default, no additional response validation is required
}
