package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class SignRequestObjectDefectiveTyp_UnitTest {

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
		env.putObject("request_object_claims", JsonParser.parseString("""
			{
				"client_id": "x509_hash:abc123",
				"response_type": "vp_token",
				"nonce": "test-nonce-123",
				"aud": "https://self-issued.me/v2"
			}
			""").getAsJsonObject());
		env.putObject("client_jwks", JsonParser.parseString(TEST_JWK_1).getAsJsonObject());
	}

	private SignedJWT sign(AbstractCondition cond) throws ParseException {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		cond.execute(env);
		return SignedJWT.parse(env.getString("request_object"));
	}

	private static void assertTypIsJwt(SignedJWT jwt) {
		assertNotNull(jwt.getHeader().getType(), "typ header should be present");
		assertEquals("jwt", jwt.getHeader().getType().getType());
	}

	@Test
	public void x5c_wrongTyp() throws ParseException {
		SignedJWT jwt = sign(new SignRequestObjectIncludeX5cHeaderWithWrongTyp());
		assertTypIsJwt(jwt);
		assertNotNull(jwt.getHeader().getX509CertChain(), "x5c must still be present so typ is the only defect");
	}

	@Test
	public void x5c_withoutTyp() throws ParseException {
		SignedJWT jwt = sign(new SignRequestObjectIncludeX5cHeaderWithoutTyp());
		assertNull(jwt.getHeader().getType(), "typ header should be absent");
		assertNotNull(jwt.getHeader().getX509CertChain(), "x5c must still be present so typ is the only defect");
	}

	@Test
	public void x5cIfAvailable_wrongTyp() throws ParseException {
		SignedJWT jwt = sign(new SignRequestObjectIncludeX5cHeaderIfAvailableWithWrongTyp());
		assertTypIsJwt(jwt);
		assertNotNull(jwt.getHeader().getX509CertChain(), "x5c must still be present so typ is the only defect");
	}

	@Test
	public void x5cIfAvailable_withoutTyp() throws ParseException {
		SignedJWT jwt = sign(new SignRequestObjectIncludeX5cHeaderIfAvailableWithoutTyp());
		assertNull(jwt.getHeader().getType(), "typ header should be absent");
		assertNotNull(jwt.getHeader().getX509CertChain(), "x5c must still be present so typ is the only defect");
	}

	@Test
	public void typOnly_wrongTyp() throws ParseException {
		SignedJWT jwt = sign(new SignRequestObjectIncludeTypHeaderWithWrongTyp());
		assertTypIsJwt(jwt);
	}

	@Test
	public void typOnly_withoutTyp() throws ParseException {
		SignedJWT jwt = sign(new SignRequestObjectIncludeTypHeaderWithoutTyp());
		assertNull(jwt.getHeader().getType(), "typ header should be absent");
	}
}
