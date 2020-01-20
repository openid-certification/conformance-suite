package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.OIDCCAddAcrValuesToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.ValidateIdTokenACRClaimAgainstAcrValuesRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Req_acr_values
@PublishTestModule(
	testName = "oidcc-ensure-request-with-acr-values-succeeds",
	displayName = "OIDCC: ensure request with acr_values succeeds",
	summary = "This test will send an acr_values containing the authorization server's advertised acr_values_supported (or if none advertised, the values '1' and '2'). The authorization must succeed, and the returned id_tokens 'SHOULD' (but is not required to) contain an acr claim with one of the requested values. The test will pass if one of the requested values is returned, otherwise it will end with a warning.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
public class OIDCCEnsureRequestWithAcrValuesSucceeds extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(OIDCCAddAcrValuesToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstAcrValuesRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.1", "OIDCC-15.1");
	}
}
