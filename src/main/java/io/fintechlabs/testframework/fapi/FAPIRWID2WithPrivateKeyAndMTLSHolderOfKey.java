package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsPrivateKeyJwt;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-with-private-key-and-mtls-holder-of-key",
	displayName = "FAPI-RW-ID2: (private key authentication with mtls holder of key)",
	summary = "This test uses two different OAuth clients, authenticates the user twice (using different variations on request object, registered redirect uri etc), tests that certificate bound access tokens are implemented correctly and tries reusing an authorization code.",
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
public class FAPIRWID2WithPrivateKeyAndMTLSHolderOfKey extends AbstractFAPIRWID2ServerTestModule {

	public FAPIRWID2WithPrivateKeyAndMTLSHolderOfKey() {
		super(new StepsConfigurationFAPI());
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndContinueOnFailure(EnsureServerConfigurationSupportsPrivateKeyJwt.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);
	}

	@Override
	protected void generateNewClientAssertion() {

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);

	}

}
