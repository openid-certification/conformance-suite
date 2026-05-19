package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class InvalidateSdJwtCredentialSignature_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private InvalidateSdJwtCredentialSignature cond;

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
		cond = new InvalidateSdJwtCredentialSignature();
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
	public void testEvaluate_invalidatesIssuerSignature() {
		createValidCredential();
		String originalCredential = env.getString("credential");

		cond.execute(env);

		String modifiedCredential = env.getString("credential");
		assertNotEquals(originalCredential, modifiedCredential);
		assertTrue(modifiedCredential.contains("~"));

		// Issuer JWT should be different
		String originalIssuerJwt = originalCredential.substring(0, originalCredential.indexOf('~'));
		String modifiedIssuerJwt = modifiedCredential.substring(0, modifiedCredential.indexOf('~'));
		assertNotEquals(originalIssuerJwt, modifiedIssuerJwt);
	}

	@Test
	public void testEvaluate_sdHashMatchesCorruptedSdJwt() throws Exception {
		createValidCredential();

		cond.execute(env);

		String modifiedCredential = env.getString("credential");
		int lastTilde = modifiedCredential.lastIndexOf('~');
		String sdJwtWithoutKb = modifiedCredential.substring(0, lastTilde + 1);
		String kbJwt = modifiedCredential.substring(lastTilde + 1);

		String expectedSdHash = ValidateSdJwtKbSdHash.getCalculatedSdHash(sdJwtWithoutKb);
		SignedJWT parsedKb = SignedJWT.parse(kbJwt);
		String actualSdHash = parsedKb.getJWTClaimsSet().getStringClaim("sd_hash");

		assertEquals(expectedSdHash, actualSdHash);
	}

	@Test
	public void testEvaluate_preservesKbJwtClaims() throws Exception {
		createValidCredential();
		String originalCredential = env.getString("credential");

		int origLastTilde = originalCredential.lastIndexOf('~');
		SignedJWT origKb = SignedJWT.parse(originalCredential.substring(origLastTilde + 1));

		cond.execute(env);

		String modifiedCredential = env.getString("credential");
		int modLastTilde = modifiedCredential.lastIndexOf('~');
		SignedJWT modKb = SignedJWT.parse(modifiedCredential.substring(modLastTilde + 1));

		assertEquals(origKb.getJWTClaimsSet().getAudience(),
			modKb.getJWTClaimsSet().getAudience());
		assertEquals(origKb.getJWTClaimsSet().getStringClaim("nonce"),
			modKb.getJWTClaimsSet().getStringClaim("nonce"));
		assertEquals(origKb.getJWTClaimsSet().getIssueTime(),
			modKb.getJWTClaimsSet().getIssueTime());
	}
}
