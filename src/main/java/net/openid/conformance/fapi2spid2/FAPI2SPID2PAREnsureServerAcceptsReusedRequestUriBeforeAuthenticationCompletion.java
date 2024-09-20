package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.AuthorizationEndpointGet;
import net.openid.conformance.condition.client.ExpectInvalidRequestUriErrorPage;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds",
	displayName = "FAPI2-Security-Profile-ID2: PAR - ensure reused request uri prior to auth completion succeeds",
	summary = "This test uses the PAR request_uri prior to authentication completion. The subsequent authentication should succeed",
	profile = "FAPI2-Security-Profile-ID2",
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
public class FAPI2SPID2PAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestUriErrorPage.class, "PAR-7.3", "PAR-4", "PAR-2.2");

		env.putString("error_callback_placeholder", env.getString("request_uri_invalid_error"));
	}

	@Override
	protected void performRedirect(String method) {
		// As per https://bitbucket.org/openid/fapi/issues/635/one-time-use-of-request_uri-causing-error
		// Issue a GET, including the 'request_uri',  to the authorization endpoint prior to a
		// successful authorization flow.
		callAndStopOnFailure(AuthorizationEndpointGet.class);

		allowPlainErrorResponseForJarm = true;
		performRedirectAndWaitForPlaceholdersOrCallback();
	}
}
