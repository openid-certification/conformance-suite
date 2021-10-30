package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallClientConfigurationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckDynamicRegistrationEndpointReturnedError;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClientExpectingFailure;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-no-mtls",
	displayName = "FAPI1-Advanced-Final: Brazil DCR no MTLS",
	summary = "Perform the DCR flow, but without presenting a TLS client certificate - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response. The client configuration endpoint GET and DELETE methods are called without a TLS certificate and must be rejected.",
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
public class FAPI1AdvancedFinalBrazilDCRNoMTLS extends AbstractFAPI1AdvancedFinalBrazilDCRMTLSIssue {

	@Override
	protected void mapToWrongMTLS() {
		env.mapKey("mutual_tls_authentication", "none_existent_key");
	}

}
