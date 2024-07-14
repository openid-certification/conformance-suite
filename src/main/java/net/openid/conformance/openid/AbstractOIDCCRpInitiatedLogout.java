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

public abstract class AbstractOIDCCRpInitiatedLogout extends AbstractOIDCCServerTest {
	protected boolean firstTime = true;
	protected boolean expectingLogoutConfirmation = false;

	@Override
	protected String currentClientString() {
		return firstTime ? "" : "Second authorization: ";
	}

	@Override
	protected void configureClient() {
		callAndStopOnFailure(CreatePostLogoutRedirectUri.class, "OIDCRIL-2", "OIDCRIL-3");
		super.configureClient();
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddPostLogoutRedirectUriToDynamicRegistrationRequest.class, "OIDCRIL-3.1");
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
			call(createAuthorizationRequestSequence()
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


	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Redirect to end session endpoint & wait for response");
		callAndStopOnFailure(CreateRandomEndSessionState.class, "OIDCRIL-2", "RFC6749A-A.5");
		callAndStopOnFailure(CreateEndSessionEndpointRequest.class, "OIDCRIL-2");
		customiseEndSessionEndpointRequest();
		callAndStopOnFailure(BuildRedirectToEndSessionEndpoint.class, "OIDCRIL-2");
		performRedirectToEndSessionEndpoint();
	}

	protected void customiseEndSessionEndpointRequest() {
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

		expectingLogoutConfirmation = true;

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo, placeholderId);
	}

	protected String createLogoutPlaceholder() {
		// override to return a placeholder id if a screenshot is required
		return null;
	}

	@Override
	public void cleanup() {
		firstTime = true; // to avoid any blocks created in cleanup being prefixed in currentClientString()
		super.cleanup();
	}
}
