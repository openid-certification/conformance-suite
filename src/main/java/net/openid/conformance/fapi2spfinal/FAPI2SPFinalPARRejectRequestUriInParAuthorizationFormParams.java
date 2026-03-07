package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBadRequestUriToRequestParameters;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

//PAR-2.1 : The request_uri authorization request parameter MUST NOT be provided in this case
@PublishTestModule(
	testName = "fapi2-security-profile-final-par-authorization-request-containing-request_uri-form-param",
	displayName = "PAR : The request_uri authorization request parameter MUST NOT be provided",
	summary = "This test sends a random request_uri form parameter in PAR authorization request and expects authorization server to return an error",
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

public class FAPI2SPFinalPARRejectRequestUriInParAuthorizationFormParams extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected  void performParAuthorizationRequestFlow() {

		//"pushed_authorization_request_form_parameters"
		callAndStopOnFailure(AddBadRequestUriToRequestParameters.class);

		callParEndpointAndStopOnFailure();

		callAndContinueOnFailure(EnsurePARInvalidRequestOrInvalidRequestObjectOrRequestUriNotSupportedError.class, Condition.ConditionResult.FAILURE, "PAR-2.1-2");

		fireTestFinished();
	}

}
