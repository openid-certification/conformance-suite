package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-brazil-dcr-no-mtls",
	displayName = "FAPI2-Security-Profile-Final: Brazil DCR no MTLS",
	summary = "Perform the DCR flow, but without presenting a TLS client certificate - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response. The client configuration endpoint GET and DELETE methods are called without a TLS certificate and must be rejected.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalBrazilDCRNoMTLS extends AbstractFAPI2SPFinalBrazilDCRMTLSIssue {

	@Override
	protected void mapToWrongMTLS() {
		env.mapKey("mutual_tls_authentication", "none_existent_key");
	}

}
