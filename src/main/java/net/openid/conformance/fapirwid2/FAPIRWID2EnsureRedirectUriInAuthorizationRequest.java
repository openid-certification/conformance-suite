package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectRedirectUriMissingErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-redirect-uri-in-authorization-request",
	displayName = "FAPI-RW-ID2: ensure redirect URI in authorization request",
	summary = "This test should result an the authorization server showing an error page saying the redirect url is missing from the request (a screenshot of which should be uploaded)",
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
public class FAPIRWID2EnsureRedirectUriInAuthorizationRequest extends AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriMissingErrorPage.class, "FAPI-R-5.2.2-9");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_missing_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();

		// Remove the redirect URL
		env.getObject("authorization_endpoint_request").remove("redirect_uri");
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

}
