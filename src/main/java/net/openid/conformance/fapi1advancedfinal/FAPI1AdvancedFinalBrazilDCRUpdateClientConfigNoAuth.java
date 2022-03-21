package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-update-client-config-no-authorization-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR update client config without authentication flow",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration)" +
		"\u2022 Registers a new client on the target authorization server and." +
		"\u2022 After the registration, a PUT will be made to the RFC7592 client to change the redirect uri (both redirect uris must be present in the software on the Brazil directory), which must succeed and an authorization flow will then be run using the new redirect uri. The contents of the 'PUT' is the dynamic registration response the server supplied, so any problems with the PUT request are due to errors in the DCR response. PUTs to the client config url to change the redirect_uri with various bad authentication will then be tried, which should all be rejected. The test will then verify the redirect uri wasn't changed.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI1AdvancedFinalBrazilDCRUpdateClientConfigNoAuth extends FAPI1AdvancedFinalBrazilDCRUpdateClientConfig{
	@Override
	public void start() {
		fireTestFinished();
	}
}
