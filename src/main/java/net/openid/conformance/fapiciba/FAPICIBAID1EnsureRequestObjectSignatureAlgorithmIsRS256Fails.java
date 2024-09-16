package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ChangeClientJwksAlgToRS256;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JWKUtil;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-signature-algorithm-is-RS256-fails",
	displayName = "FAPI-CIBA-ID1: Ensure request_object signature algorithm is RS256 fails",
	summary = "This test should end with the backchannel authorization server returning an error message that the request is invalid.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsRS256Fails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void onConfigure() {
		String alg = JWKUtil.getAlgFromClientJwks(env);
		if (!alg.equals("PS256")) { // FAPI only allows ES256 and PS256
			// This throws an exception: the test will stop here
			fireTestSkipped("This test requires RSA keys to be performed, the alg in client configuration is '%s' so this test is being skipped. If your server does not support PS256 then this will not prevent you certifying.".formatted(alg));
		}
	}

	@Override
	protected void performAuthorizationRequest() {
		// create a copy of the jwks so we can restore the original one when creating any client assertion
		env.putObject("client_jwks_rs256", env.getObject("client_jwks").deepCopy());
		env.mapKey("client_jwks", "client_jwks_rs256");
		callAndContinueOnFailure(ChangeClientJwksAlgToRS256.class, Condition.ConditionResult.FAILURE, "FAPI-CIBA-7.10");

		super.performAuthorizationRequest();
	}

	@Override
	protected void addClientAuthenticationToBackchannelRequest() {
		env.unmapKey("client_jwks");
		super.addClientAuthenticationToBackchannelRequest();
	}
}
