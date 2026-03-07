package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-par-without-duplicate-parameters",
	displayName = "FAPI2-Security-Profile-Final: PAR request without duplicate parameters",
	summary = "This test makes a PAR request and calls the authorization endpoint passing only the client_id and request_uri parameters - which must succeed, as per section 5 of https://datatracker.ietf.org/doc/html/rfc9101 - this intent was confirmed by the FAPI working group in https://bitbucket.org/openid/fapi/issues/315/par-certification-question-must-servers",
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

public class FAPI2SPFinalParWithoutDuplicateParameters extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performPARRedirectWithRequestUri() {
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}
}
