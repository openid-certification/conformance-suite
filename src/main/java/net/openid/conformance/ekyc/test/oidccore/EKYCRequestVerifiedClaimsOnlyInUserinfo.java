package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddOneVerifiedClaimInUserinfoOnlyToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.EnsureIdTokenDoesNotContainVerifiedClaims;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-request-only-in-userinfo",
	displayName = "eKYC Server Test - Request verified_claims in userinfo only",
	summary = "Request only one claim, selected from the list of claims_in_verified_claims_supported, only in userinfo, " +
		"without requesting any other verification element and expect a happy path flow." +
		"verified_claims must not be included in id_tokens.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCRequestVerifiedClaimsOnlyInUserinfo extends AbstractEKYCTestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddOneVerifiedClaimInUserinfoOnlyToAuthorizationEndpointRequest.class, Condition.ConditionResult.WARNING, "IA-6.1");
	}

	@Override
	protected void processVerifiedClaimsInIdToken() {
		callAndStopOnFailure(EnsureIdTokenDoesNotContainVerifiedClaims.class, "IA-6.1");
	}
}
