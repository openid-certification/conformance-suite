package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.EnsureParHTTPError;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-par-attempt-invalid-http-method",
	displayName = "PAR : try to use an invalid http method ",
	summary = "This test tries to use an invalid http method and expects authorization server to return an error",
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

@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })

//PAR-2.3.3 : If the request did not use POST, the authorization server shall return 405 Method Not Allowed HTTP error response.
public class FAPI2SPFinalPARRejectInvalidHttpVerb extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performParAuthorizationRequestFlow() {
		env.putString(CallPAREndpoint.HTTP_METHOD_KEY, "PUT");

		callParEndpointAndStopOnFailure();

		callAndContinueOnFailure(EnsureParHTTPError.class, Condition.ConditionResult.FAILURE, "PAR-2.3");

		fireTestFinished();
	}
}
