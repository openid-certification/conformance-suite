package net.openid.conformance.ekycid3.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddOneVerifiedClaimInIdTokenOnlyToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.EnsureUserinfoDoesNotContainVerifiedClaims;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekycid3-server-request-only-in-idtoken",
	displayName = "eKYC Server Test - Request verified_claims in id_token only",
	summary = "Request only one claim, selected from the list of claims_in_verified_claims_supported, only in id_token, " +
		"without requesting any other verification element and expect a happy path flow." +
		"verified_claims must not be included in userinfo responses.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class EKYCID3RequestVerifiedClaimsOnlyInIdToken extends AbstractEKYCID3TestWithOIDCCore {

	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddOneVerifiedClaimInIdTokenOnlyToAuthorizationEndpointRequest.class, Condition.ConditionResult.WARNING, "IAID3-6.1");
	}

	@Override
	protected void processVerifiedClaimsInUserinfo() {
		//as we didn't request verified_claims in userinfo, response must not contain verified_claims
		callAndContinueOnFailure(EnsureUserinfoDoesNotContainVerifiedClaims.class, Condition.ConditionResult.FAILURE, "IAID3-6");
	}
}
