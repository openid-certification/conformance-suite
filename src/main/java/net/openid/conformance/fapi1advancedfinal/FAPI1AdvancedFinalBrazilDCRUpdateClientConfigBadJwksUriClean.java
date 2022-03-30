package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-update-client-config-bad-jwks-uri-clean",
	displayName = "FAPI1-Advanced-Final: Brazil DCR update client config",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration).\n" +
		"\u2022 Register a new client on the target authorization server.\n" +
		"\u2022 The test will then use a PUT to try and change the jwks_uri to an invalid one, the server must return an 'invalid_client_metadata' error.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	}
)
public class FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUriClean
	extends FAPI1AdvancedFinalBrazilDCRUpdateClientConfigBadJwksUri{

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void validateDcrResponseScope() {
		// Not needed as scope field is optional
	}

	@Override
	protected void copyFromDynamicRegistrationTemplateToClientConfiguration() {
		// Not needed as scope field is optional
	}
}
