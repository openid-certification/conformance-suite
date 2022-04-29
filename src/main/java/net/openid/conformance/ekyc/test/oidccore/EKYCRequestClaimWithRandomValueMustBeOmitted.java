package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallUserInfoEndpoint;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ExtractUserInfoFromUserInfoEndpointResponse;
import net.openid.conformance.ekyc.condition.client.AddClaimWithRandomValueToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.EnsureIdTokenDoesNotContainVerifiedClaims;
import net.openid.conformance.ekyc.condition.client.EnsureUserinfoDoesNotContainVerifiedClaims;
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
		" The verified_claims must be omitted from responses completely as the value cannot be fulfilled.",
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
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallUserInfoEndpoint.class);
		call(exec().mapKey("endpoint_response", "userinfo_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		callAndStopOnFailure(ExtractUserInfoFromUserInfoEndpointResponse.class);

		callAndContinueOnFailure(EnsureUserinfoDoesNotContainVerifiedClaims.class,  Condition.ConditionResult.FAILURE, "IA-7.7.3");
		eventLog.endBlock();

	}
}
