package net.openid.conformance.fapirwid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CreateBadRedirectUri;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-registered-redirect-uri",
	displayName = "FAPI-RW-ID2: ensure registered redirect URI",
	summary = "This test uses an unregistered redirect uri. The authorization server should display an error saying the redirect uri is invalid, a screenshot of which should be uploaded.",
	profile = "FAPI-RW-ID2",
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
public class FAPIRWID2EnsureRegisteredRedirectUri extends AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {

		// create a random redirect URI
		callAndStopOnFailure(CreateBadRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	@Override
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		Integer http_status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");

		fireTestFinished();
	}

	@Override
	protected void performParAuthorizationRequestFlow() {
		super.performParAuthorizationRequestFlow();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "FAPI-R-5.2.2-8");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void processCallback() {
		throw new TestFailureException(getId(), "The authorization server called the registered redirect uri. This should not have happened as the client provided a bad redirect_uri in the request.");
	}
}
