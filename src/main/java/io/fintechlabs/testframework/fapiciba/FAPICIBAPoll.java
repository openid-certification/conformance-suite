package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddMTLSClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import io.fintechlabs.testframework.sequence.client.AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll",
	displayName = "FAPI-CIBA: Poll mode",
	summary = "This test requires two different clients registered under the FAPI-CIBA profile for the 'poll' mode. The test authenticates the user twice (using different variations on the authorisation request etc), tests that certificate bound access tokens are implemented correctly. Do not respond to the request until the test enters the 'WAITING' state.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
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

public class FAPICIBAPoll extends AbstractFAPICIBA {
	@Variant(name = "mtls")
	public void setupMTLS() {
		addBackchannelClientAuthentication = AddMTLSClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToTokenEndpointRequest.class;
	}

	@Variant(name = "private-key-jwt-and-mtls-holder-of-key")
	public void setupPrivateKeyJwt() {
		addBackchannelClientAuthentication = AddPrivateKeyJWTClientAuthenticationToBackchannelRequest.class;
		addTokenEndpointClientAuthentication = AddPrivateKeyJWTClientAuthenticationToTokenEndpointRequest.class;
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		waitForPollingAuthenticationToComplete(delaySeconds);
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		// Nothing extra to setup for Poll
	}
}
