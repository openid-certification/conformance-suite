package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddVerifiedClaimsToAuthorizationEndpointRequestWithEssentialFalse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekyc-server-happypath-essentialfalse",
	displayName = "eKYC Happy Path Server Test - Essential false",
	summary = "Request only one claim, selected from the list of claims_in_verified_claims_supported, " +
		"without requesting any other verification element and expect a happy path flow. Uses {\"essential\": false} " +
		"instead of null when request the claim.",
	profile = "OIDCC",
	configurationFields = {
		"trust_framework",
		"verified_claim_names",
		"ekyc.unverified_claims_names",
		"ekyc.verified_claims_names",
		"ekyc.request_schemas",
		"ekyc.response_schemas"
	}
)
public class EKYCHappyPathTestEssentialFalse extends AbstractEKYCTestWithOIDCCore {
	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddVerifiedClaimsToAuthorizationEndpointRequestWithEssentialFalse.class, Condition.ConditionResult.WARNING, "IA-6");
	}
}
