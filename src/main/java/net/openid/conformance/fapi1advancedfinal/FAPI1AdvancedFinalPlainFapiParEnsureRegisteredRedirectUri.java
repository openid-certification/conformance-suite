package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CreateBadRedirectUriByAppending;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@PublishTestModule(
	testName = "fapi1-advanced-final-plain-fapi-par-ensure-registered-redirect-uri",
	displayName = "FAPI1-Advanced-Final: ensure registered redirect URI",
	summary = "This test uses an unregistered redirect uri. The authorization server may allow this as per RFC 9126 Section 2.4 or may display an error saying the redirect uri is invalid, a screenshot of which should be uploaded.",
	profile = "FAPI1-Advanced-Final",
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
// Allowing the OP to accept a 'redirect_uri' that has not been previously registered is for 'plain_fapi' + PAR only.
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = { "by_value" })

public class FAPI1AdvancedFinalPlainFapiParEnsureRegisteredRedirectUri extends AbstractFAPI1AdvancedFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	protected boolean parError = false;

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		if (getVariant(FAPI1FinalOPProfile.class) != FAPI1FinalOPProfile.PLAIN_FAPI) {
			fireTestSkipped("This test is only available for the 'plain_fapi' profile.");
		}

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
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "FAPI1-BASE-5.2.2-8");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void processCallback() {
		if (parError) {
			throw new TestFailureException(getId(), "The authorization server called the registered redirect uri. This should not have happened as the client provided a bad redirect_uri in the request.");
		}

		super.processCallback();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		callbackEndpoint = "callback/" + env.getString("bad_redirect_path");

		return super.handleHttp(path, req, res, session, requestParts);
	}
}
