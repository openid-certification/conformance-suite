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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
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

	private void putRequestParams(boolean algOnKey, String authorizationEncryptedResponseAlg) {
		JsonObject jwk = JsonParser.parseString(encryptionKey.toPublicJWK().toJSONString()).getAsJsonObject();
		if (algOnKey) {
			jwk.addProperty("alg", "ECDH-ES");
		}
		JsonArray keys = new JsonArray();
		keys.add(jwk);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keys);
		JsonObject clientMetadata = new JsonObject();
		clientMetadata.add("jwks", jwks);
		if (authorizationEncryptedResponseAlg != null) {
			clientMetadata.addProperty("authorization_encrypted_response_alg", authorizationEncryptedResponseAlg);
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
		putRequestParams(true, null);

		cond.execute(env);

		assertThat(encryptedResponseAlg()).isEqualTo("ECDH-ES");
	}

	@Test
	public void testEvaluate_algMissingFallsBackToAuthorizationEncryptedResponseAlg() throws Exception {
		putRequestParams(false, "ECDH-ES");

		cond.execute(env);

		assertThat(encryptedResponseAlg()).isEqualTo("ECDH-ES");
	}

	@Test
	public void testEvaluate_algMissingWithNoFallbackThrowsError() {
		putRequestParams(false, null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
