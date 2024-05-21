package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-br-v1-brazil-dcr-no-mtls",
	displayName = "FAPI1-Advanced-Final-Br-v1: Brazil DCR no MTLS",
	summary = "Perform the DCR flow, but without presenting a TLS client certificate - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response. The client configuration endpoint GET and DELETE methods are called without a TLS certificate and must be rejected.",
	profile = "FAPI1-Advanced-Final-Br-v1",
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
public class FAPI1AdvancedFinalBrV1BrazilDCRNoMTLS extends AbstractFAPI1AdvancedFinalBrV1BrazilDCRMTLSIssue {

	@Override
	protected void mapToWrongMTLS() {
		env.mapKey("mutual_tls_authentication", "none_existent_key");
	}

}
