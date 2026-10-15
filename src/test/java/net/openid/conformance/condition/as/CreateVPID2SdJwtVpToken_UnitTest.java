package net.openid.conformance.condition.as;

import com.authlete.sd.SDJWT;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateVPID2SdJwtVpToken_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateVPID2SdJwtVpToken cond;

	@BeforeEach
	public void setUp() {
		// PreGeneratedJwks requires owner_sub / owner_iss.
		env.putString("owner_sub", "unit-test-sub");
		env.putString("owner_iss", "unit-test-iss");
		cond = new CreateVPID2SdJwtVpToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_missingSigningJwkSignsWithTheBuiltInPidProviderCertificate() throws Exception {
		env.putObject("config", new JsonObject());
		env.putObject("client", JsonParser.parseString("{\"client_id\":\"https://verifier.example\"}").getAsJsonObject());
		env.putString("nonce", "n");

		cond.execute(env);

		SDJWT sdJwt = SDJWT.parse(env.getString("credential"));
		com.nimbusds.jwt.SignedJWT issuerJwt = com.nimbusds.jwt.SignedJWT.parse(sdJwt.getCredentialJwt());
		assertEquals(TestKeysAndCerts.getSdJwtPidSigner().getJwk().getX509CertChain(), issuerJwt.getHeader().getX509CertChain());
		assertTrue(issuerJwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(TestKeysAndCerts.getSdJwtPidSigner().getJwk().toECPublicKey())));
	}

	@Test
	public void testEvaluate_unparseableSigningJwkErrorReferencesConfigField() {
		env.putObjectFromJsonString("config", "credential.signing_jwk", """
			{"kty": "not-a-real-kty"}""");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration"),
			"expected message to reference the unparseable 'Signing JWK' config field but was: " + err.getMessage());
	}

}
