package net.openid.conformance.fapi2spid2;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationPARRequestParameters;
import net.openid.conformance.condition.as.RemoveStateFromAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-ensure-authorization-response-with-invalid-missing-state-fails",
	displayName = "FAPI2-Security-Profile-ID2: sends an authorization endpoint response with a missing state value which the client should reject",
	summary = "This test should end with the client displaying an error message that the state value in the authorization endpoint response is invalid. If the client does not send a state value the test result will be SKIPPED.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)



public class FAPI2SPID2ClientTestEnsureAuthorizationResponseWithInvalidMissingStateFails extends AbstractFAPI2SPID2ClientTest {

	protected boolean removedState = false;

	@Override
	protected void endTestIfRequiredParametersAreMissing() {
		String state = env.getString(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, CreateEffectiveAuthorizationPARRequestParameters.STATE);
		if(Strings.isNullOrEmpty(state)) {
			fireTestSkipped("This test is being skipped as it relies on the client supplying an OPTIONAL state value - since none is supplied, this can not be tested. PKCE prevents CSRF so this is acceptable and will not prevent certification.");
		}
	}

	@Override
	protected void addCustomValuesToAuthorizationResponse() {
		String state = env.getString(CreateAuthorizationEndpointResponseParams.ENV_KEY, CreateAuthorizationEndpointResponseParams.STATE);
		if(!Strings.isNullOrEmpty(state)) {
			callAndContinueOnFailure(RemoveStateFromAuthorizationEndpointResponseParams.class, Condition.ConditionResult.INFO);
			removedState = true;
		}
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void createAuthorizationEndpointResponse() {
		super.createAuthorizationEndpointResponse();
		if(removedState) {
			startWaitingForTimeout();
		}
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		if(removedState) {
			throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an invalid authorization response (" +
				getAuthorizationResponseErrorMessage() + ")");
		} else {
			return super.tokenEndpoint(requestId);
		}
	}

	@Override
	protected Object userinfoEndpoint(String requestId) {
		if(removedState) {
			throw new TestFailureException(getId(), "Client has incorrectly called userinfo_endpoint after receiving an invalid authorization response (" +
				getAuthorizationResponseErrorMessage() + ")");
		} else {
			return super.userinfoEndpoint(requestId);
		}
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		if(removedState) {
			throw new TestFailureException(getId(), "Client has incorrectly called accounts endpoint after receiving an invalid authorization response (" +
				getAuthorizationResponseErrorMessage() + ")");
		} else {
			return super.accountsEndpoint(requestId);
		}
	}

	protected String getAuthorizationResponseErrorMessage() {
		return "Removed state from the authorization response";
	}
}
