package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.GenerateFakeMTLSCertificate;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-bad-mtls",
	displayName = "FAPI1-Advanced-Final: Brazil DCR bad MTLS",
	summary = "Perform the DCR flow, but presenting a TLS client certificate that should not be trusted - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response. The client configuration endpoint GET and DELETE methods are called with a bad TLS certificate and must be rejected.",
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
public class FAPI1AdvancedFinalBrazilDCRBadMTLS extends AbstractFAPI1AdvancedFinalBrazilDCRMTLSIssue {

	@Override
	protected void mapToWrongMTLS() {
		env.mapKey("mutual_tls_authentication", "fake_mutual_tls_authentication");
	}

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(GenerateFakeMTLSCertificate.class);
		super.callRegistrationEndpoint();
	}

}
