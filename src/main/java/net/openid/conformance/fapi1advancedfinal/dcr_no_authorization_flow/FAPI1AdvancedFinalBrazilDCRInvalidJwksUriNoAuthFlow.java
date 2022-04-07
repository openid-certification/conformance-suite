package net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow;

import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRInvalidJwksUri;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-invalid-jwks-uri-no-authorization-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR Invalid JWKS URI",
	summary = "Perform the DCR flow, but requesting a jwks uri not hosted on the OB Brazil directory - the server must reject the registration attempt.",
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
public class FAPI1AdvancedFinalBrazilDCRInvalidJwksUriNoAuthFlow extends FAPI1AdvancedFinalBrazilDCRInvalidJwksUri {

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

}
