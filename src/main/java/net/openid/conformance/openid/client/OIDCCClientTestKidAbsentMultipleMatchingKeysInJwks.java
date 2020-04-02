package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.OIDCCGenerateServerJWKsMultipleSigningsKeyWithNoKeyIds;
import net.openid.conformance.condition.as.SetServerSigningAlgToRS256;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-kid-absent-multiple-jwks",
	displayName = "OIDCC: Relying party test. Server JWKS contains multiple possible keys but no 'kid's ",
	summary = "This test always uses RS256 signing algorithm. " +
		"Identify that the 'kid' value is missing from the JOSE header and that the Issuer publishes " +
		"multiple keys in its JWK Set document (referenced by 'jwks_uri'). " +
		"The RP can do one of two things; " +
		"reject the ID Token since it can not by using the kid determined which key to use to verify the signature. " +
		"Or it can just test all possible keys and hit upon one that works, which it will in this case." +
		" Corresponds to rp-id_token-kid-absent-multiple-jwks test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks extends AbstractOIDCCClientTest {

	@Override
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKsMultipleSigningsKeyWithNoKeyIds.class, "OIDCC-10.1");
	}

	@Override
	protected void validateConfiguredServerJWKS() {
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
	}

	/**
	 * this test would not work with HS* or none, so always requiring RS256
	 */
	@Override
	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(SetServerSigningAlgToRS256.class);
	}

	/**
	 * For this test the client may or may not respond.
	 * @param requestId
	 * @return
	 */
	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId) {
		Object returnValue = super.handleAuthorizationEndpointRequest(requestId);
		if(responseType.includesIdToken()) {
			startWaitingForTimeout();
		}
		return returnValue;
	}

	/**
	 * For this test the client may or may not respond.
	 * @param requestId
	 * @return
	 */
	@Override
	protected Object authorizationCodeGrantType(String requestId) {
		Object returnValue = super.authorizationCodeGrantType(requestId);
		startWaitingForTimeout();
		return returnValue;
	}
}
