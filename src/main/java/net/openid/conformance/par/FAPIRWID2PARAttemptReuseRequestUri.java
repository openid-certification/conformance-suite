package net.openid.conformance.par;

import net.openid.conformance.condition.client.BuildRequestToAuthorizationEndpointWithRequestUri;
import net.openid.conformance.condition.client.ExpectInvalidRequestUriErrorPage;
import net.openid.conformance.fapi.AbstractFAPIRWID2ServerTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-7.3 : An attacker could replay a request URI captured from a legitimate authorization request.
// In order to cope with such attacks, the AS SHOULD make the request URIs one-time use.
@PublishTestModule(
	testName = "fapi-rw-id2-par-attempt-reuse-request_uri",
	displayName = "PAR : try to reuse a request_uri ",
	summary = "This test tries to reuse a request_uri and expects authorization server to return an error",
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
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value"
})
public class FAPIRWID2PARAttemptReuseRequestUri extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void createPlaceholder() {
		// This may be too strict, as one-time use is only a 'should' in
		// https://tools.ietf.org/html/draft-ietf-oauth-par-03#section-7.3
		callAndStopOnFailure(ExpectInvalidRequestUriErrorPage.class, "PAR-7.3", "PAR-4", "PAR-2.2");

		env.putString("error_callback_placeholder", env.getString("request_uri_invalid_error"));

		eventLog.endBlock();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Attempting reuse of request_uri and testing if Authorization server returns error in callback");
		// We're testing that reuse of the request_uri is refused.
		callAndStopOnFailure(BuildRequestToAuthorizationEndpointWithRequestUri.class);

		env.putBoolean("second_call", true);

		performRedirectAndWaitForPlaceholdersOrCallback();
	}

}
