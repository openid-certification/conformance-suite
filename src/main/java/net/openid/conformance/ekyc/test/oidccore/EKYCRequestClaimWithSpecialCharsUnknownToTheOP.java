package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.ekyc.condition.client.AddUnknownVerifiedClaimWithSpecialCharsToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	Based on Yes.com test: "Test: Claim that is unknown to the OP; containing special characters in its name. Expected result: Claim is omitted from response"
 */
@PublishTestModule(
	testName = "ekyc-server-unknown-claim-specialchars-omitted",
	displayName = "eKYC Server Test - Unknown essential claim omitted from response",
	summary = "This test requests one known claim, selected from the list of claims_in_verified_claims_supported, " +
		"and one random claim name (unknown to the OP), with special chars in its name, and expects a happy path flow." +
		" The unknown claim must be omitted from responses.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCRequestClaimWithSpecialCharsUnknownToTheOP extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddUnknownVerifiedClaimWithSpecialCharsToAuthorizationEndpointRequest.class);
	}

	//Note: as we don't check if all requested claims were returned by default, no additional response validation is performed
}
