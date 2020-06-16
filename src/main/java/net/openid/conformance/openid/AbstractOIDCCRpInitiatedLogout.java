package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddPostLogoutRedirectUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPromptNoneToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRedirectToEndSessionEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface;
import net.openid.conformance.condition.client.CreateEndSessionEndpointRequest;
import net.openid.conformance.condition.client.CreatePostLogoutRedirectUri;
import net.openid.conformance.condition.client.CreateRandomEndSessionState;
import net.openid.conformance.condition.client.ExtractSessionStateFromAuthorizationResponse;

public abstract class AbstractOIDCCRpInitiatedLogout extends AbstractOIDCCServerTest {
	protected boolean firstTime = true;

	@Override
	protected String currentClientString() {
		return firstTime ? "" : "Second authorization: ";
	}

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreatePostLogoutRedirectUri.class, "OIDCSM-5", "OIDCSM-5.1.1");
		super.configureClient();
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddPostLogoutRedirectUriToDynamicRegistrationRequest.class, "OIDCSM-5.1.1");
	}

	@Override
    protected void createAuthorizationRequest() {
        // python includes the offline_access scope in all authorization requests; I checked with Roland (see 9th June
        // 2020 email) and there was no reason he could remember for doing this and he suspected it was likely a C&P
        // error, so java does not include offline_access.

        if (firstTime) {
            super.createAuthorizationRequest();
        } else {
            // with prompt=none this time
            call(new CreateAuthorizationRequestSteps(formPost)
                .then(condition(AddPromptNoneToAuthorizationEndpointRequest.class).requirements("OIDCC-3.1.2.1", "OIDCC-15.1")));
        }
    }

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		// use a longer state value to check OP doesn't corrupt it
		env.putInteger("requested_state_length", 128);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		if (firstTime) {
			firstTime = false;
			super.onAuthorizationCallbackResponse();
		} else {
			performGenericAuthorizationEndpointErrorResponseValidation();

			callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			fireTestFinished();
		}
	}


	protected void onPostAuthorizationFlowComplete() {
		callAndStopOnFailure(ExtractSessionStateFromAuthorizationResponse.class, "OIDCSM-3");

		eventLog.startBlock("Redirect to end session endpoint & wait for response");
		callAndStopOnFailure(CreateRandomEndSessionState.class, "OIDCSM-5", "RFC6749A-A.5");
		callAndStopOnFailure(CreateEndSessionEndpointRequest.class, "OIDCSM-5");
		callAndStopOnFailure(BuildRedirectToEndSessionEndpoint.class, "OIDCSM-5");
		performRedirectToEndSessionEndpoint();
	}

	protected void performRedirectToEndSessionEndpoint() {
		String placeholderId = createLogoutPlaceholder();
		String redirectTo = env.getString("redirect_to_end_session_endpoint");

		if (placeholderId != null) {
			waitForPlaceholders();
		}

		eventLog.log(getName(), args("msg", "Redirecting to end session endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo, placeholderId);
	}

	protected String createLogoutPlaceholder() {
		// override to return a placeholder id if a screenshot is required
		return null;
	}

}
