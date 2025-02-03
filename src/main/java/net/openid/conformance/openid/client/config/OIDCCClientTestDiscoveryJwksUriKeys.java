package net.openid.conformance.openid.client.config;

import net.openid.conformance.condition.as.AddRandomJwksUriToServerConfiguration;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.apache.commons.lang3.RandomStringUtils;


@PublishTestModule(
	testName = "oidcc-client-test-discovery-jwks-uri-keys",
	displayName = "OIDCC: Relying party discovery test, jwks_uri support",
	summary = "The client is expected to retrieve OpenID Provider Configuration Information " +
		"and send a request to jwks_uri obtained from OP configuration." +
		"The jwks_uri endpoint changes every execution and retrieving the configuration is a requirement." +
		"Corresponds to rp-discovery-jwks_uri-keys test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestDiscoveryJwksUriKeys extends AbstractOIDCCClientTest {

	/**
	 * We will append this random value to the end of the jwks uri to make it random per test
	 */
	private String randomJwksUriSuffix = RandomStringUtils.secure().nextAlphabetic(10);

	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedDiscoveryRequest && receivedJwksRequest) {
			fireTestFinished();
			return true;
		}
		return super.finishTestIfAllRequestsAreReceived();
	}

	@Override
	protected void configureServerConfiguration() {
		super.configureServerConfiguration();
		env.putString("random_jwks_uri_suffix", randomJwksUriSuffix);
		callAndStopOnFailure(AddRandomJwksUriToServerConfiguration.class);
	}

	@Override
	protected String getJwksPath() {
		return "jwks" + randomJwksUriSuffix;
	}


	@Override
	protected void checkIfDiscoveryCalled(String path) {
		if(!receivedDiscoveryRequest){
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path + " before the discovery endpoint call");
		}
	}

	@Override
	protected void checkIfJWKCalled(String path) {
		if(!receivedJwksRequest){
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path + " before the jwks endpoint call");
		}
	}
}
