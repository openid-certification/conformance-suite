package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-invalid-jwks-by-value-clean",
	displayName = "FAPI1-Advanced-Final: Brazil DCR Invalid JWKS by value",
	summary = "Perform the DCR flow, but passing a jwks by value - the server must reject the registration attempt.",
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
public class FAPI1AdvancedFinalBrazilDCRInvalidJwksByValueClean extends FAPI1AdvancedFinalBrazilDCRInvalidJwksByValue{

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void validateDcrResponseScope() {
		// Not needed as scope field is optional
	}

}
