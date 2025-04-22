package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestUriError;
import net.openid.conformance.condition.client.ExpectLoginPage;
import net.openid.conformance.condition.client.WarningAboutRequestUriError;

import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Collections;
import java.util.List;

@PublishTestModule(
	testName = "fapi2-security-profile-final-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds",
	displayName = "FAPI2-Security-Profile-Final: PAR - ensure reused request uri prior to auth completion succeeds",
	summary = "This test checks that authorization servers that enforce one-time use of `request_uri` values do so at the point of authorization, not at the point of visiting the authorization endpoint. This is achieved by visiting the authorization endpoint twice with the same 'request_uri' value. On the first visit no login should be attempted. On the second visit the login should be attempted and is expected to succeed. On error a screenshot showing the resulting error needs to be uploaded  This test is as per FAPI 2.0 Security Profile 5.3.2.2 Note 3. This is a recommendation and as such any failure of this test will result in a warning.\n\nYou may need to clear any cookies for the authorization server before running this test, to remove any existing login session and hence ensure the initial visit to the login page does not automatically authenticate the user.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion extends AbstractFAPI2SPFinalServerTestModule {

	private static final int initialLoginVisitTimeoutSecs = 120;

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectLoginPage.class);
	}

	@Override
	protected void performRedirect(String method) {
		// Initial visit to the authorization endpoint. The user should take no action.
		redirect(env.getString("redirect_to_authorization_endpoint"));

		// Wait for the login page to be visited.
		boolean loginPageVisited = false;

		for (int attempts = 0; attempts < initialLoginVisitTimeoutSecs/2; attempts++) {
			setStatus(Status.WAITING);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			setStatus(Status.RUNNING);

			List<String> visitedUrls = getBrowser().getVisited();

			if (visitedUrls.contains(env.getString("redirect_to_authorization_endpoint"))) {
				loginPageVisited = true;
				break;
			}
		}

		if (! loginPageVisited) {
			throw new RuntimeException("The initial authorization server login page was not visited within the " + initialLoginVisitTimeoutSecs + " seconds timeout period.");
		}
		eventLog.endBlock();

		eventLog.startBlock("Make second request to authorization endpoint");

		// Proceed with the regular authorization flow, revisiting the authorization endpoint and logging in.
		performRedirectAndWaitForPlaceholdersOrCallback("login_page_placeholder");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)
		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {
			super.onAuthorizationCallbackResponse();
		} else {
			// If we get an error back from the authorization server:
			// - It must be a 'invalid_request_uri' error
			// - It must have the correct state we supplied
			callAndContinueOnFailure(WarningAboutRequestUriError.class, Condition.ConditionResult.WARNING, "FAPI2-SP-FINAL-5.3.2.2");
			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(EnsureInvalidRequestUriError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");
			fireTestFinished();
		}
	}

	@Override
	protected void processCallback() {
		// The user must not authenticate until the second visit to the login page.
		// Reject the callback until the login page has been visited a second time.
		if (Collections.frequency( getBrowser().getVisited(), env.getString("redirect_to_authorization_endpoint")) < 2) {
			throw new RuntimeException("The user was authenticated on the initial visit to login page. This must not be attempted until the second visit. " +
				"If no login was attempted then please delete related cookies or logout from the server before re-running.");
		}

		super.processCallback();
	}
}
