package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimWithShortPurposeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

/*
	purpose: OPTIONAL. String describing the purpose for obtaining a certain End-User Claim from the OP.
	The purpose MUST NOT be shorter than 3 characters or longer than 300 characters.
	If this rule is violated, the authentication request MUST fail and the OP return an error invalid_request to the RP.
 */
@PublishTestModule(
	testName = "ekyc-server-short-purpose-invalid-request",
	displayName = "eKYC Server Test - Short purpose leads to invalid_request error",
	summary = """
		This test requests one known claim, selected from the list of claims_in_verified_claims_supported, \
		 with a purpose which is only 2 characters long.

		 As per section 6.1 of the spec, the authentication request MUST fail and the OP return an error invalid_request to the RP.\
		""",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCTooShortPurposeLeadsToInvalidRequestResponse extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndStopOnFailure(AddVerifiedClaimWithShortPurposeToAuthorizationEndpointRequest.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		performGenericAuthorizationEndpointErrorResponseValidation();	//<- is this general enough to be applicable to ekyc too?
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "IA-6.1");
		//test ends here
		fireTestFinished();
	}
}
