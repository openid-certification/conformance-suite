package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObjectJSON;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateMultiSignedRequestObjectBadTyp_UnitTest {

	// Same key and self-signed certificate as CreateMultiSignedRequestObject_UnitTest.TEST_JWK_1
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

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		JsonObject claims = JsonParser.parseString("""
			{
				"response_type": "vp_token",
				"nonce": "test-nonce-123",
				"aud": "https://self-issued.me/v2",
				"expected_origins": ["https://example.com"]
			}
			""").getAsJsonObject();
		env.putObject("request_object_claims", claims);
		env.putObject("client_jwks", JsonParser.parseString(TEST_JWK_1).getAsJsonObject());
		// the second signer only needs a distinct kid; the same key and certificate are sufficient
		env.putObject("client2_jwks", JsonParser.parseString(TEST_JWK_1.replace("\"key1\"", "\"key2\"")).getAsJsonObject());
		env.putString("client_id", "x509_hash:abc123");
		env.putString("client2_id", "x509_hash:def456");
	}

	private JWSObjectJSON parseResult() throws ParseException {
		return JWSObjectJSON.parse(env.getObject("request_object_json").toString());
	}

	@Test
	public void wrongTyp_isJwtInEverySignature() throws ParseException {
		CreateMultiSignedRequestObjectWithWrongTyp cond = new CreateMultiSignedRequestObjectWithWrongTyp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		cond.execute(env);

		JWSObjectJSON parsed = parseResult();
		assertEquals(2, parsed.getSignatures().size());
		for (JWSObjectJSON.Signature sig : parsed.getSignatures()) {
			Map<String, Object> header = sig.getHeader().toJSONObject();
			assertEquals("jwt", header.get("typ"), "every signature must carry the wrong typ, but found: " + header);
			assertTrue(header.containsKey("client_id"), "client_id must still be in the protected header");
			assertTrue(header.containsKey("x5c"), "x5c must still be present so typ is the only defect");
		}
	}

	@Test
	public void withoutTyp_hasNoTypInAnySignature() throws ParseException {
		CreateMultiSignedRequestObjectWithoutTyp cond = new CreateMultiSignedRequestObjectWithoutTyp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		cond.execute(env);

		JWSObjectJSON parsed = parseResult();
		assertEquals(2, parsed.getSignatures().size());
		for (JWSObjectJSON.Signature sig : parsed.getSignatures()) {
			Map<String, Object> header = sig.getHeader().toJSONObject();
			assertFalse(header.containsKey("typ"), "no signature may carry typ, but found: " + header);
			assertTrue(header.containsKey("client_id"), "client_id must still be in the protected header");
			assertTrue(header.containsKey("x5c"), "x5c must still be present so typ is the only defect");
		}
	}

	@Test
	public void baseCondition_stillSendsCorrectTyp() throws ParseException {
		CreateMultiSignedRequestObject cond = new CreateMultiSignedRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		cond.execute(env);

		for (JWSObjectJSON.Signature sig : parseResult().getSignatures()) {
			assertEquals("oauth-authz-req+jwt", sig.getHeader().toJSONObject().get("typ"),
				"the refactoring must not change what the normal condition sends");
		}
	}
}
