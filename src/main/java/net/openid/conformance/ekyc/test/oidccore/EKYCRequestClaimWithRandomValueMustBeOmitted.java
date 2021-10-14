package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.ExtractUserInfoFromUserInfoEndpointResponse;
import net.openid.conformance.ekyc.condition.client.AddClaimWithRandomValueToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.AddUnknownVerifiedClaimToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.EnsureIdTokenDoesNotContainVerifiedClaims;
import net.openid.conformance.ekyc.condition.client.EnsureUserinfoDoesNotContainVerifiedClaims;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	Based on Yes.com test: "Test: Check if value limitations are evaluated correctly. Note: value is chosen such that it cannot be fulfilled.
							Expected result: verified_claims must be omitted completely."
 */
@PublishTestModule(
	testName = "ekyc-server-one-claim-with-random-value-omitted",
	displayName = "eKYC Server Test - Only one claim requested with random value, verified_claims omitted from response",
	summary = "This test requests one known claim, selected from the list of claims_in_verified_claims_supported, " +
		" but with a random value (a UUID) that cannot be fullfilled and expects a happy path flow." +
		" The verified_claims must be omitted from responses completely as the value cannot be fulfilled.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCRequestClaimWithRandomValueMustBeOmitted extends BaseEKYCTestWithOIDCCore {

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
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
		env.putString("userinfo_endpoint_response", env.getString("resource_endpoint_response"));
		callAndStopOnFailure(ExtractUserInfoFromUserInfoEndpointResponse.class);

		callAndContinueOnFailure(EnsureUserinfoDoesNotContainVerifiedClaims.class,  Condition.ConditionResult.FAILURE, "IA-7.7.3");
		eventLog.endBlock();

	}
}
