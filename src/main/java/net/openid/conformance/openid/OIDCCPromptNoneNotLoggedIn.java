package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPromptNoneToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-prompt-none-NotLoggedIn.json
@PublishTestModule(
	testName = "oidcc-prompt-none-not-logged-in",
	displayName = "OIDCC: prompt=none when not logged in",
	summary = "This test calls the authorization endpoint with prompt=none, expecting that no recent enough authentication is present to enable a silent login and hence the OP will redirect back with an error as per section 3.1.2.6 of OpenID Connect. Please remove any cookies you may have received from the OpenID Provider before proceeding.",
	profile = "OIDCC"
)
public class OIDCCPromptNoneNotLoggedIn extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		// use a longer state value to check OP doesn't corrupt it in the error response
		env.putInteger("requested_state_length", 128);

		return super.createAuthorizationRequestSequence()
			.then(condition(AddPromptNoneToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1"));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		performGenericAuthorizationEndpointErrorResponseValidation();

		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		fireTestFinished();
	}

}
