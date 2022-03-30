package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-no-redirect-uri-clean",
	displayName = "FAPI1-Advanced-Final: Brazil DCR no redirect uri",
	summary = "Perform the DCR flow, but without including a redirect uri in the request body - the server must reject the registration attempt.",
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
public class FAPI1AdvancedFinalBrazilDCRNoRedirectUriClean extends FAPI1AdvancedFinalBrazilDCRNoRedirectUri {

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
