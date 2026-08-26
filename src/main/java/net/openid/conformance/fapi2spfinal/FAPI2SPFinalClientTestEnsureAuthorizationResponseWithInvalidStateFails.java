package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidStateToAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-ensure-authorization-response-with-invalid-state-fails",
	displayName = "FAPI2-Security-Profile-Final: sends an authorization endpoint response with an invalid state value.",
	summary = "This test should end with the client displaying an error message that the state value in the authorization endpoint response is invalid",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)
public class FAPI2SPFinalClientTestEnsureAuthorizationResponseWithInvalidStateFails extends AbstractFAPI2SPFinalClientExpectNothingAfterAuthorizationResponse {

	@Override
	protected void addCustomValuesToAuthorizationResponse() {
		callAndContinueOnFailure(AddInvalidStateToAuthorizationEndpointResponseParams.class, Condition.ConditionResult.INFO);
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}
	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Added invalid state to the authorization response";
	}
}
