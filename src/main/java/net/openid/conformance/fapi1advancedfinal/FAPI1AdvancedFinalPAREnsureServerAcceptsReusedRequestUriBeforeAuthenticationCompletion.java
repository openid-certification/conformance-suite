package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.AuthorizationEndpointGet;
import net.openid.conformance.condition.client.ExpectInvalidRequestUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds",
	displayName = "FAPI1-Advanced-Final:  PAR - ensure reused request uri prior to auth completion succeeds",
	summary = "This test uses the PAR request_uri prior to authentication completion. The subsequent authentication should succeed",
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

@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})

public class FAPI1AdvancedFinalPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion extends AbstractFAPI1AdvancedFinalServerTestModule {
	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectInvalidRequestUriErrorPage.class, "PAR-7.3", "PAR-4", "PAR-2.2");

		env.putString("error_callback_placeholder", env.getString("request_uri_invalid_error"));
	}

	@Override
	protected void performRedirect(String method) {
		// As per https://bitbucket.org/openid/fapi/issues/635/one-time-use-of-request_uri-causing-error
		// Issue a GET, including the 'request_uri', to the authorization endpoint prior to proceeding
		// to the authorization flow. This should succeed.
		callAndStopOnFailure(AuthorizationEndpointGet.class);

		allowPlainErrorResponseForJarm = true;
		performRedirectAndWaitForPlaceholdersOrCallback();
	}
}
