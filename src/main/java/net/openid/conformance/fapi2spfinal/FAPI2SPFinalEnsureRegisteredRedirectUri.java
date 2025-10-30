package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateBadRedirectUriByAppending;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-registered-redirect-uri",
	displayName = "FAPI2-Security-Profile-Final: ensure registered redirect URI",
	summary = "This test uses an unregistered redirect uri. The authorization server should display an error saying the redirect uri is invalid, a screenshot of which should be uploaded.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
// The 'redirect_uri' is required to be pre-registered. This is not the case for 'plain_fapi'.
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi" })

public class FAPI2SPFinalEnsureRegisteredRedirectUri extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		// create a random redirect URI
		callAndStopOnFailure(CreateBadRedirectUriByAppending.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	@Override
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");
	}

	@Override
	protected void performParAuthorizationRequestFlow() {
		super.performParAuthorizationRequestFlow();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "PAR-2.1-3");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void processCallback() {
		throw new TestFailureException(getId(), "The authorization server called the registered redirect uri. This should not have happened as the client provided a bad redirect_uri in the request.");
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals(env.getString("bad_redirect_path"))) {
			throw new TestFailureException(getId(), "The authorization server redirected the user to the requested but randomised/unregistered redirect uri. This must not happen as the provided redirect uri could not have been registered.");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}

	}
}
