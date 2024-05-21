package net.openid.conformance.openid.client.config;

import net.openid.conformance.condition.as.AddRandomSuffixToIssuerInServerConfiguration;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import jakarta.servlet.http.HttpServletResponse;


@PublishTestModule(
	testName = "oidcc-client-test-discovery-webfinger-url",
	displayName = "OIDCC: Relying party test, webfinger using url syntax",
	summary = "The client is expected to use WebFinger (RFC7033) and " +
		"OpenID Provider Issuer Discovery to determine the location of the OpenID Provider configuration" +
		" and send a request to the .well-known/openid-configuration endpoint. " +
		" The discovery should be done using URL syntax as user input identifier. " +
		" The resource URI MUST have the following value: " +
		"'https://HOST/YOUR_TEST_ALIAS/oidcc-client-test-discovery-webfinger-url' " +
		"(HOST should be the hostname:port for the suite)." +
		"Corresponds to rp-discovery-webfinger-url test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestDiscoveryWebfingerURL extends AbstractOIDCCClientTest {

	@Override
	protected void configureServerConfiguration() {
		super.configureServerConfiguration();
		callAndStopOnFailure(AddRandomSuffixToIssuerInServerConfiguration.class);
	}

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedDiscoveryRequest) {
			fireTestFinished();
			return true;
		}
		return super.finishTestIfAllRequestsAreReceived();
	}

	@Override
	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse) {
		String discoveryUrl = env.getString("discoveryUrl");
		String suffix = discoveryUrl.substring(discoveryUrl.length() - (10 + ".well-known/openid-configuration".length()) );
		if (path.endsWith(suffix)) {
			receivedDiscoveryRequest = true;
			return handleDiscoveryEndpointRequest();
		}
		return super.handleClientRequestForPath(requestId, path, servletResponse);
	}

	@Override
	protected void validateWebfingerRequestResource(String resourcePrefix) {
		if(!"https".equals(resourcePrefix)) {
			throw new TestFailureException(getId(), "This test expects a webfinger request using URL syntax " +
				"(e.g https://example.com/test-alias/"+this.getName()+")");
		}
	}
}
