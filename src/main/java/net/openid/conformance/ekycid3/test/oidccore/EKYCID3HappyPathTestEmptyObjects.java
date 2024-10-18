package net.openid.conformance.ekycid3.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.ekyc.condition.client.AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingEmptyJsonObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ekycid3-server-happypath-emptyobject",
	displayName = "eKYC Happy Path Server Test - Empty Object",
	summary = "Request only one claim, selected from the list of claims_in_verified_claims_supported, " +
		"without requesting any other verification element and expect a happy path flow. Uses empty objects instead " +
		"of 'null' when requesting claim.",
	profile = "OIDCC",
	configurationFields = {
		"trust_framework",
		"verified_claim_names"
	}
)
public class EKYCID3HappyPathTestEmptyObjects extends AbstractEKYCID3TestWithOIDCCore {
	@Override
	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingEmptyJsonObject.class, Condition.ConditionResult.WARNING, "IAID3-6");
	}
}
