package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-no-mtls-clean",
	displayName = "FAPI1-Advanced-Final: Brazil DCR no MTLS",
	summary = "Perform the DCR flow, but without presenting a TLS client certificate - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response. The client configuration endpoint GET and DELETE methods are called without a TLS certificate and must be rejected.",
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
public class FAPI1AdvancedFinalBrazilDCRNoMTLSClean extends FAPI1AdvancedFinalBrazilDCRNoMTLS{
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
