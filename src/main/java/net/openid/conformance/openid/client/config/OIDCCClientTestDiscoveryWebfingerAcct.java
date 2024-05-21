package net.openid.conformance.openid.client.config;

import net.openid.conformance.condition.as.AddRandomSuffixToIssuerInServerConfiguration;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import jakarta.servlet.http.HttpServletResponse;


@PublishTestModule(
	testName = "oidcc-client-test-discovery-webfinger-acct",
	displayName = "OIDCC: Relying party test, webfinger using acct syntax",
	summary = "The client is expected to use WebFinger (RFC7033) and " +
		"OpenID Provider Issuer Discovery to determine the location of the OpenID Provider configuration" +
		" and send a request to the .well-known/openid-configuration endpoint. " +
		"The discovery should be done using acct URI syntax as user input identifier. " +
		" Note that the acct value must adhere to the pattern " +
		"'acct:YOUR_TEST_ALIAS.oidcc-client-test-discovery-webfinger-acct@HOST' " +
		"(HOST should be the hostname:port for the suite but it's not validated by the suite)." +
		"Corresponds to rp-discovery-webfinger-acct test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestDiscoveryWebfingerAcct extends AbstractOIDCCClientTest {

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
		if(!"acct".equals(resourcePrefix)) {
			throw new TestFailureException(getId(), "This test expects a webfinger request using acct syntax");
		}
	}
}
