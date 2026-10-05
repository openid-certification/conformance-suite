package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class SignRequestObjectIncludeX5cHeaderIfAvailable_UnitTest {

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

	private SignRequestObjectIncludeX5cHeaderIfAvailable cond;

	@BeforeEach
	public void setUp() {
		cond = new SignRequestObjectIncludeX5cHeaderIfAvailable();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("request_object_claims", JsonParser.parseString("""
			{
				"client_id": "my-pre-registered-client",
				"response_type": "vp_token",
				"nonce": "test-nonce-123",
				"aud": "https://self-issued.me/v2"
			}
			""").getAsJsonObject());
	}

	@Test
	public void keyWithX5c_hasTypAndX5c() throws ParseException {
		env.putObject("client_jwks", JsonParser.parseString(TEST_JWK_1).getAsJsonObject());

		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString("request_object"));
		assertNotNull(jwt.getHeader().getType(), "OID4VP section 5 requires the typ header in request objects");
		assertEquals("oauth-authz-req+jwt", jwt.getHeader().getType().getType());
		assertNotNull(jwt.getHeader().getX509CertChain());
		assertFalse(jwt.getHeader().getX509CertChain().isEmpty());
	}

	@Test
	public void keyWithoutX5c_hasTypAndNoX5c() throws ParseException {
		JsonObject jwks = JsonParser.parseString(TEST_JWK_1).getAsJsonObject();
		jwks.getAsJsonArray("keys").get(0).getAsJsonObject().remove("x5c");
		env.putObject("client_jwks", jwks);

		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString("request_object"));
		assertNotNull(jwt.getHeader().getType(), "OID4VP section 5 requires the typ header in request objects");
		assertEquals("oauth-authz-req+jwt", jwt.getHeader().getType().getType());
		assertNull(jwt.getHeader().getX509CertChain(), "x5c is only included when the key has a certificate chain");
	}
}
