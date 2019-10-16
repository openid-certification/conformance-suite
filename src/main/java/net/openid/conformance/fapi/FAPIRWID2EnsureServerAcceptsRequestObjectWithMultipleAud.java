package net.openid.conformance.fapi;

import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddMultipleAudToRequestObject;
import net.openid.conformance.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-request-object-with-multiple-aud-succeeds",
	displayName = "FAPI-RW-ID2: ensure request object with multiple aud succeeds",
	summary = "This test pass aud value as an array containing good and bad values then server must accept it.",
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
public class FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		if (whichClient == 2) {
			callAndStopOnFailure(AddIatToRequestObject.class);
		}
		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(AddMultipleAudToRequestObject.class, "RFC7519-4.1.3");

		callAndStopOnFailure(AddIssToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void performPostAuthorizationFlow() {
		// call the token endpoint and complete the flow

		createAuthorizationCodeRequest();

		requestAuthorizationCode();

		requestProtectedResource();

		fireTestFinished();
	}
}
