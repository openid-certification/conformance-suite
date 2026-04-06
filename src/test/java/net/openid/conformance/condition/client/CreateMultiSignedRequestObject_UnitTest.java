package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObjectJSON;
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

import java.text.ParseException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CreateMultiSignedRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateMultiSignedRequestObject cond;

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

	// Second key - uses the same cert as key1 but with different kid (sufficient for testing multi-signed structure)
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
		cond = new CreateMultiSignedRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_success() throws ParseException {
		JsonObject claims = JsonParser.parseString("""
			{
				"client_id": "x509_hash:abc123",
				"response_type": "vp_token",
				"nonce": "test-nonce-123",
				"dcql_query": {"credentials": [{"id": "cred1"}]},
				"aud": "https://self-issued.me/v2",
				"expected_origins": ["https://example.com"]
			}
			""").getAsJsonObject();

		env.putObject("request_object_claims", claims);
		env.putObject("client_jwks", JsonParser.parseString(TEST_JWK_1).getAsJsonObject());
		env.putObject("client2_jwks", JsonParser.parseString(TEST_JWK_2).getAsJsonObject());
		env.putString("client_id", "x509_hash:abc123");
		env.putString("client2_id", "x509_hash:def456");

		cond.execute(env);

		JsonObject result = env.getObject("request_object_json");
		assertThat(result).isNotNull();

		// Verify JWS JSON Serialization structure
		assertThat(result.has("payload")).isTrue();
		assertThat(result.has("signatures")).isTrue();

		JsonArray signatures = result.getAsJsonArray("signatures");
		assertThat(signatures).hasSize(2);

		// Each signature should have protected and signature fields
		for (int i = 0; i < signatures.size(); i++) {
			JsonObject sig = signatures.get(i).getAsJsonObject();
			assertThat(sig.has("protected")).isTrue();
			assertThat(sig.has("signature")).isTrue();
		}

		// Verify the payload does NOT contain client_id (it should be in protected headers)
		String serialized = result.toString();
		JWSObjectJSON parsed = JWSObjectJSON.parse(serialized);
		String payloadStr = parsed.getPayload().toString();
		JsonObject payloadJson = JsonParser.parseString(payloadStr).getAsJsonObject();
		assertThat(payloadJson.has("client_id")).isFalse();
		assertThat(payloadJson.has("nonce")).isTrue();
		assertThat(payloadJson.has("expected_origins")).isTrue();

		// Verify each signature's protected header contains client_id
		for (JWSObjectJSON.Signature sig : parsed.getSignatures()) {
			Map<String, Object> headerParams = sig.getHeader().toJSONObject();
			assertThat(headerParams).containsKey("client_id");
			assertThat(headerParams).containsKey("typ");
			assertThat(headerParams.get("typ")).isEqualTo("oauth-authz-req+jwt");
		}
	}

	@Test
	public void testEvaluate_missingClientJwks() {
		JsonObject claims = JsonParser.parseString("""
			{"client_id": "x509_hash:abc123", "nonce": "test"}
			""").getAsJsonObject();

		env.putObject("request_object_claims", claims);
		env.putObject("client2_jwks", JsonParser.parseString(TEST_JWK_2).getAsJsonObject());
		env.putString("client_id", "x509_hash:abc123");
		env.putString("client2_id", "x509_hash:def456");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClient2Jwks() {
		JsonObject claims = JsonParser.parseString("""
			{"client_id": "x509_hash:abc123", "nonce": "test"}
			""").getAsJsonObject();

		env.putObject("request_object_claims", claims);
		env.putObject("client_jwks", JsonParser.parseString(TEST_JWK_1).getAsJsonObject());
		env.putString("client_id", "x509_hash:abc123");
		env.putString("client2_id", "x509_hash:def456");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
