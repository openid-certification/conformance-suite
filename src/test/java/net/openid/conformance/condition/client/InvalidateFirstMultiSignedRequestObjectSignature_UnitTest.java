package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class InvalidateFirstMultiSignedRequestObjectSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private InvalidateFirstMultiSignedRequestObjectSignature cond;

	private CreateMultiSignedRequestObject createCond;

	private static final String TEST_JWK_1 = """
		{
			"keys": [{
				"kty": "EC",
				"crv": "P-256",
				"x": "MeTgsS50BR72Lj--MxFPTL7DNKxClCymdqo1hZ8_09U",
				"y": "5cHwkpG7iLvtsqA41gNowdAt4Ro83vdE-P6eWGmegLc",
				"d": "gwmApx70vcVlRzQid2uY-ooMjtm331NmCvtOuIOr_6I",
				"use": "sig",
				"kid": "key1",
				"alg": "ES256",
				"x5c": [
					"MIIBkjCCATegAwIBAgIUZkRih1mNAs9PfQphhjLx8O2Uej8wCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSeDV0LXMyNTYtdW5pdC10ZXN0MCAXDTI2MDIwODE1NTEwMVoYDzIxMjYwMTE1MTU1MTAxWjAdMRswGQYDVQQDDBJ4NXQtczI1Ni11bml0LXRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQx5OCxLnQFHvYuP74zEU9MvsM0rEKULKZ2qjWFnz/T1eXB8JKRu4i77bKgONYDaMHQLeEaPN73RPj+nlhpnoC3o1MwUTAdBgNVHQ4EFgQUQYMPimHGw8fD+nAw5hXN1tLeHE8wHwYDVR0jBBgwFoAUQYMPimHGw8fD+nAw5hXN1tLeHE8wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAgyNkETTSsp/nkhXKjNETK4UGQXSayRAFtZ6hJSyKIOUCIQCIW7UskVfn6zliot/KzfmqY1XDjaTf6kzqhv5YBlRmtg=="
				]
			}]
		}
		""";

	private static final String TEST_JWK_2 = """
		{
			"keys": [{
				"kty": "EC",
				"crv": "P-256",
				"x": "MeTgsS50BR72Lj--MxFPTL7DNKxClCymdqo1hZ8_09U",
				"y": "5cHwkpG7iLvtsqA41gNowdAt4Ro83vdE-P6eWGmegLc",
				"d": "gwmApx70vcVlRzQid2uY-ooMjtm331NmCvtOuIOr_6I",
				"use": "sig",
				"kid": "key2",
				"alg": "ES256",
				"x5c": [
					"MIIBkjCCATegAwIBAgIUZkRih1mNAs9PfQphhjLx8O2Uej8wCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSeDV0LXMyNTYtdW5pdC10ZXN0MCAXDTI2MDIwODE1NTEwMVoYDzIxMjYwMTE1MTU1MTAxWjAdMRswGQYDVQQDDBJ4NXQtczI1Ni11bml0LXRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQx5OCxLnQFHvYuP74zEU9MvsM0rEKULKZ2qjWFnz/T1eXB8JKRu4i77bKgONYDaMHQLeEaPN73RPj+nlhpnoC3o1MwUTAdBgNVHQ4EFgQUQYMPimHGw8fD+nAw5hXN1tLeHE8wHwYDVR0jBBgwFoAUQYMPimHGw8fD+nAw5hXN1tLeHE8wDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNJADBGAiEAgyNkETTSsp/nkhXKjNETK4UGQXSayRAFtZ6hJSyKIOUCIQCIW7UskVfn6zliot/KzfmqY1XDjaTf6kzqhv5YBlRmtg=="
				]
			}]
		}
		""";

	@BeforeEach
	public void setUp() {
		cond = new InvalidateFirstMultiSignedRequestObjectSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		createCond = new CreateMultiSignedRequestObject();
		createCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void createMultiSignedRequestObject() {
		JsonObject claims = JsonParser.parseString("""
			{
				"response_type": "vp_token",
				"nonce": "test-nonce-123",
				"dcql_query": {"credentials": [{"id": "cred1"}]},
				"aud": "https://self-issued.me/v2"
			}
			""").getAsJsonObject();

		env.putObject("request_object_claims", claims);
		env.putObject("client_jwks", JsonParser.parseString(TEST_JWK_1).getAsJsonObject());
		env.putObject("client2_jwks", JsonParser.parseString(TEST_JWK_2).getAsJsonObject());
		env.putString("client_id", "x509_hash:abc123");
		env.putString("client2_id", "x509_hash:def456");

		createCond.execute(env);
	}

	@Test
	public void testEvaluate_onlyFirstSignatureCorrupted() {
		createMultiSignedRequestObject();

		JsonObject original = env.getObject("request_object_json").deepCopy();
		String originalFirstSig = OIDFJSON.getString(original.getAsJsonArray("signatures").get(0).getAsJsonObject().get("signature"));
		String originalSecondSig = OIDFJSON.getString(original.getAsJsonArray("signatures").get(1).getAsJsonObject().get("signature"));

		cond.execute(env);

		JsonObject result = env.getObject("request_object_json");
		JsonArray signatures = result.getAsJsonArray("signatures");

		assertThat(signatures).hasSize(2);

		String newFirstSig = OIDFJSON.getString(signatures.get(0).getAsJsonObject().get("signature"));
		String newSecondSig = OIDFJSON.getString(signatures.get(1).getAsJsonObject().get("signature"));

		// First signature should be changed
		assertThat(newFirstSig).isNotEqualTo(originalFirstSig);
		// Second signature should be unchanged
		assertThat(newSecondSig).isEqualTo(originalSecondSig);
	}

	@Test
	public void testEvaluate_failsWithSingleSignature() {
		// Create a request object with only one signature
		JsonObject requestObjectJson = new JsonObject();
		requestObjectJson.addProperty("payload", "eyJ0ZXN0IjoiMSJ9");
		JsonArray signatures = new JsonArray();
		JsonObject sig = new JsonObject();
		sig.addProperty("protected", "eyJhbGciOiJFUzI1NiJ9");
		sig.addProperty("signature", "dGVzdA");
		signatures.add(sig);
		requestObjectJson.add("signatures", signatures);

		env.putObject("request_object_json", requestObjectJson);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
