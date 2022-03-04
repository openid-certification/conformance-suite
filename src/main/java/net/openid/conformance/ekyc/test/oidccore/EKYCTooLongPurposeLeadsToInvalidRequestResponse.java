package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimWithLongPurposeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	purpose: OPTIONAL. String describing the purpose for obtaining a certain End-User Claim from the OP.
	The purpose MUST NOT be shorter than 3 characters or longer than 300 characters.
	If this rule is violated, the authentication request MUST fail and the OP return an error invalid_request to the RP.
 */
@PublishTestModule(
	testName = "ekyc-server-long-purpose-invalid-request",
	displayName = "eKYC Server Test - Purpose over 300 characters leads to invalid_request error",
	summary = "This test requests one known claim, selected from the list of claims_in_verified_claims_supported, " +
		" with a purpose longer than 300 characters." +
		" The authentication request MUST fail and the OP return an error invalid_request to the RP.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCTooLongPurposeLeadsToInvalidRequestResponse extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddVerifiedClaimWithLongPurposeToAuthorizationEndpointRequest.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		performGenericAuthorizationEndpointErrorResponseValidation();	//<- is this general enough to be applicable to ekyc too?
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "IA-6.1");
		//test ends here
		fireTestFinished();
	}
}
