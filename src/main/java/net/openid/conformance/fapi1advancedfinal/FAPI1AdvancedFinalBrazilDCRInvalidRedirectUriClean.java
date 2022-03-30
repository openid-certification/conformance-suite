package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-invalid-redirect-uri-clean",
	displayName = "FAPI1-Advanced-Final: Brazil DCR Invalid Redirect URI",
	summary = "Perform the DCR flow, but requesting a redirect uri not present in the software statement - the server must reject the registration attempt.",
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
public class FAPI1AdvancedFinalBrazilDCRInvalidRedirectUriClean extends FAPI1AdvancedFinalBrazilDCRInvalidRedirectUri{

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
