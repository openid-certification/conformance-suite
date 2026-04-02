package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class InvalidateSdJwtKbSignature_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private InvalidateSdJwtKbSignature cond;

	private static final String SIGNING_KEY = """
		{
		    "kty": "EC",
		    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
		    "use": "sig",
		    "crv": "P-256",
		    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
		    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
		    "alg": "ES256"
		}""";

	@BeforeEach
	public void setUp() {
		cond = new InvalidateSdJwtKbSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void createValidCredential() {
		env.putString("nonce", "test-nonce-12345");
		JsonObject client = new JsonObject();
		client.addProperty("client_id", "https://example.com/verifier");
		env.putObject("client", client);
		env.putString("base_url", "https://example.com/test");
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", SIGNING_KEY);

		CreateSdJwtKbCredential creator = new CreateSdJwtKbCredential();
		creator.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		creator.execute(env);
	}

	@Test
	public void testEvaluate_invalidatesKbJwtSignature() {
		createValidCredential();
		String originalCredential = env.getString("credential");

		cond.execute(env);

		String modifiedCredential = env.getString("credential");
		assertNotEquals(originalCredential, modifiedCredential);
		assertTrue(modifiedCredential.contains("~"));
	}

	@Test
	public void testEvaluate_preservesIssuerJwtAndDisclosures() {
		createValidCredential();
		String originalCredential = env.getString("credential");

		cond.execute(env);

		String modifiedCredential = env.getString("credential");

		// The issuer JWT and disclosures should be unchanged
		int origLastTilde = originalCredential.lastIndexOf('~');
		int modLastTilde = modifiedCredential.lastIndexOf('~');
		String originalPrefix = originalCredential.substring(0, origLastTilde + 1);
		String modifiedPrefix = modifiedCredential.substring(0, modLastTilde + 1);
		assertEquals(originalPrefix, modifiedPrefix);

		// Only the KB-JWT should differ
		String originalKbJwt = originalCredential.substring(origLastTilde + 1);
		String modifiedKbJwt = modifiedCredential.substring(modLastTilde + 1);
		assertNotEquals(originalKbJwt, modifiedKbJwt);
	}
}
