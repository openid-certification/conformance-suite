package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.client.AddAcrClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.openbanking.OBCodeIdTokenWithMTLS;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-code-id-token-with-mtls",
	displayName = "FAPI-RW: code id_token (MTLS authentication)",
	profile = "FAPI-RW",
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
public class FAPIRWCodeIdTokenWithMTLS extends OBCodeIdTokenWithMTLS {

	@Override
	protected void performPreAuthorizationSteps() {
		/* none necessary; this is here to disable the OB specific steps */
	}

	@Override
	protected void performProfileIdTokenValidation() {
		/* nothing yet; this is here to disable the OB specific checks */
	}

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(AddAcrClaimToAuthorizationEndpointRequest.class);
	}

}
