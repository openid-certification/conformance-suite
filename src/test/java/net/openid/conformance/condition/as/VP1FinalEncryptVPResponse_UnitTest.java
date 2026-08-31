package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VP1FinalEncryptVPResponse_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VP1FinalEncryptVPResponse cond;

	private ECKey encryptionKey;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VP1FinalEncryptVPResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		encryptionKey = new ECKeyGenerator(Curve.P_256).keyID("enc-key-1").generate();

		JsonObject responseParams = new JsonObject();
		responseParams.addProperty("vp_token", "test_vp_token");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, responseParams);
	}

	private void putRequestParams(String keyAlg, String metadataAlg) {
		JsonObject jwk = JsonParser.parseString(encryptionKey.toPublicJWK().toJSONString()).getAsJsonObject();
		if (keyAlg != null) {
			jwk.addProperty("alg", keyAlg);
		}
		JsonArray keys = new JsonArray();
		keys.add(jwk);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keys);
		JsonObject clientMetadata = new JsonObject();
		clientMetadata.add("jwks", jwks);
		if (metadataAlg != null) {
			clientMetadata.addProperty("authorization_encrypted_response_alg", metadataAlg);
		}
		JsonObject requestParams = new JsonObject();
		requestParams.add("client_metadata", clientMetadata);
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, requestParams);
	}

	private String encryptedResponseAlg() throws Exception {
		String response = env.getString("direct_post_request_form_parameters", "response");
		assertThat(response).isNotNull();
		return JWEObject.parse(response).getHeader().getAlgorithm().getName();
	}

	@Test
	public void testEvaluate_algOnKey_noError() throws Exception {
		putRequestParams("ECDH-ES", null);

		cond.execute(env);

		assertThat(encryptedResponseAlg()).isEqualTo("ECDH-ES");
	}

	@Test
	public void testEvaluate_algMissingFallsBackToAuthorizationEncryptedResponseAlg() throws Exception {
		putRequestParams(null, "ECDH-ES");

		cond.execute(env);

		assertThat(encryptedResponseAlg()).isEqualTo("ECDH-ES");
	}

	@Test
	public void testEvaluate_algMissingAndFallbackAlgIsForAnotherKeyTypeThrowsError() {
		// the key is EC, so an RSA alg would make encrypt() select a different key than the one
		// whose thumbprint the mdoc session transcript commits to
		putRequestParams(null, "RSA-OAEP-256");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_algMissingAndFallbackAlgIsNotAsymmetricThrowsError() {
		// "dir" is in neither the RSA nor the ECDH-ES family, so no key type can be derived
		putRequestParams(null, "dir");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_algMissingWithNoFallbackThrowsError() {
		putRequestParams(null, null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
