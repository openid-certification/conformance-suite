package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.testmodule.PublishTestModule;

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
public class FAPI2SPFinalParWithoutDuplicateParameters extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected void performPARRedirectWithRequestUri() {
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}
}
