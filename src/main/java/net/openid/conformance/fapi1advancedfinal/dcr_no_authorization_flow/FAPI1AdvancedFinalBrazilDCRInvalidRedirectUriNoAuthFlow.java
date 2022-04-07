package net.openid.conformance.fapi1advancedfinal.dcr_no_authorization_flow;

import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRInvalidRedirectUri;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-invalid-redirect-uri-no-authorization-flow",
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
public class FAPI1AdvancedFinalBrazilDCRInvalidRedirectUriNoAuthFlow extends FAPI1AdvancedFinalBrazilDCRInvalidRedirectUri {

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}
}
