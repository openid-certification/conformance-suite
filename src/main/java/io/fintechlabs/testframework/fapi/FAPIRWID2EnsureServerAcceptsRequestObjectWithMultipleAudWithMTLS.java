package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-request-object-with-multiple-aud-succeeds-with-mtls",
	displayName = "FAPI-RW-ID2: ensure request object with multiple aud succeeds (MTLS authentication)",
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
public class FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAudWithMTLS extends AbstractFAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud {

	public FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAudWithMTLS() {
		super(new StepsConfigurationFAPI());
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
