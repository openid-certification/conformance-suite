package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateVpClientMetadataEncryptionForHaip_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVpClientMetadataEncryptionForHaip cond;

	// A valid ECDH-ES P-256 public key in JWK format
	private static final String ECDH_ES_P256_KEY = """
		{
			"kty": "EC",
			"crv": "P-256",
			"alg": "ECDH-ES",
			"use": "enc",
			"kid": "enc-key-1",
			"x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw",
			"y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c"
		}""";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVpClientMetadataEncryptionForHaip();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private JsonObject buildValidConfig() {
		return JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\"],"
				+ "\"jwks\": {\"keys\": [" + ECDH_ES_P256_KEY + "]}"
				+ "}}"
		).getAsJsonObject();
	}

	@Test
	public void testEvaluate_validHaipConfig() {
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, buildValidConfig());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_validWithAdditionalEncValues() {
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\", \"A128CBC-HS256\"],"
				+ "\"jwks\": {\"keys\": [" + ECDH_ES_P256_KEY + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingA128GCM() {
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A256GCM\"],"
				+ "\"jwks\": {\"keys\": [" + ECDH_ES_P256_KEY + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingA256GCM() {
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\"],"
				+ "\"jwks\": {\"keys\": [" + ECDH_ES_P256_KEY + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingEncValuesSupported() {
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"jwks\": {\"keys\": [" + ECDH_ES_P256_KEY + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_wrongKeyAlgorithm() {
		// RSA-OAEP key instead of ECDH-ES
		String rsaKey = """
			{
				"kty": "EC",
				"crv": "P-256",
				"alg": "RSA-OAEP",
				"kid": "wrong-alg-key",
				"x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw",
				"y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c"
			}""";
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\"],"
				+ "\"jwks\": {\"keys\": [" + rsaKey + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_wrongCurve() {
		// P-384 curve instead of P-256
		String p384Key = """
			{
				"kty": "EC",
				"crv": "P-384",
				"alg": "ECDH-ES",
				"kid": "wrong-curve-key",
				"x": "iA7aWw_DOiGHyDMRIKL-s7BDfLpSwCH7r3KFVOaTBhs8sFXmftSGOhV8BLfcKqPi",
				"y": "0gY_e31UAnVUmQsSxcUP10BUdoEWQYuh0IfE4295BRJFA1BxICXwfXHWOXLQbQF0"
			}""";
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\"],"
				+ "\"jwks\": {\"keys\": [" + p384Key + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingJwks() {
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\"]"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_emptyKeysArray() {
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\"],"
				+ "\"jwks\": {\"keys\": []}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noAlgOnKey() {
		// Key without alg field - should not match ECDH-ES requirement
		String noAlgKey = """
			{
				"kty": "EC",
				"crv": "P-256",
				"kid": "no-alg-key",
				"x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw",
				"y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c"
			}""";
		JsonObject config = JsonParser.parseString(
			"{\"client_metadata\": {"
				+ "\"encrypted_response_enc_values_supported\": [\"A128GCM\", \"A256GCM\"],"
				+ "\"jwks\": {\"keys\": [" + noAlgKey + "]}"
				+ "}}"
		).getAsJsonObject();
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
