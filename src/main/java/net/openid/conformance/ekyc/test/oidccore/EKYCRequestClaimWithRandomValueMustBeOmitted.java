package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddClaimWithRandomValueToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.EnsureIdTokenDoesNotContainVerifiedClaims;
import net.openid.conformance.ekyc.condition.client.EnsureUserinfoDoesNotContainRequestedClaimInVerifiedClaims;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	Based on Yes.com test: "Test: Check if value limitations are evaluated correctly. Note: value is chosen such that it cannot be fulfilled.
							Expected result: verified_claims must be omitted completely."
 */
@PublishTestModule(
	testName = "ekyc-server-one-claim-with-random-value-omitted",
	displayName = "eKYC Server Test - Only one claim requested with random value, verified_claims omitted from response",
	summary = "This test requests one known claim, selected from the list of claims_in_verified_claims_supported, " +
		" but with a random value (a UUID) that cannot be fullfilled and expects the authorization to succeed." +
		" The claims object in verified_claims must be omitted or must not contain the requested claim from the response as the value cannot be fulfilled.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCRequestClaimWithRandomValueMustBeOmitted extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddClaimWithRandomValueToAuthorizationEndpointRequest.class);
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		callAndContinueOnFailure(EnsureIdTokenDoesNotContainVerifiedClaims.class, Condition.ConditionResult.FAILURE,"IA-7.7.3");
	}

	@Override
	protected void processVerifiedClaimsInIdToken() {
		// we don't expect verified claims so don't attempt to extract
	}

	@Override
	protected void ensureReturnedVerifiedClaimsMatchOPMetadata(boolean isUserinfo) {
		// don't do any processing, here
	}


	@Override
	protected void validateUserinfoVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(EnsureUserinfoDoesNotContainRequestedClaimInVerifiedClaims.class, Condition.ConditionResult.FAILURE, "IA-5.7.4");
	}

}
