package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValidatePDPSignedMetadataAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPSignedMetadataAlg cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPSignedMetadataAlg();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putHeader(String headerJson) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("header", JsonParser.parseString(headerJson).getAsJsonObject());
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	@Test
	public void validAlg_succeeds() {
		putHeader("{ \"alg\": \"RS256\" }");
		cond.execute(env);
	}

	@Test
	public void algNone_fails() {
		// An unsecured JWT gets its own message: it is not an unusable algorithm, it is no signature at all.
		putHeader("{ \"alg\": \"none\" }");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("unsecured"), e.getMessage());
	}

	@Test
	public void algNoneAnyCase_fails() {
		putHeader("{ \"alg\": \"NONE\" }");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("unsecured"), e.getMessage());
	}

	@Test
	public void algMissing_fails() {
		putHeader("{ \"typ\": \"JWT\" }");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no `alg` header parameter"), e.getMessage());
	}

	@Test
	public void unsupportedAlg_fails() {
		// An algorithm name the JOSE library cannot map to a key type: previously this passed here
		// and then crashed the signature verification with a NullPointerException.
		putHeader("{ \"alg\": \"FOO256\" }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void jweKeyManagementAlg_fails() {
		// §9.1.3 requires JWS. These are JWE algorithms, and KeyType.forAlgorithm() maps each of them
		// onto a key type, so a key-type test alone would have accepted them here.
		for (String alg : new String[]{"dir", "A128KW", "RSA-OAEP-256", "ECDH-ES"}) {
			putHeader("{ \"alg\": \"" + alg + "\" }");
			assertThrows(ConditionError.class, () -> cond.execute(env), alg + " must be rejected");
		}
	}

	@Test
	public void ecAlgs_succeed() {
		putHeader("{ \"alg\": \"ES256\" }");
		cond.execute(env);
		putHeader("{ \"alg\": \"ES256K\" }");
		cond.execute(env);
	}

	@Test
	public void rsaPssAlg_succeeds() {
		putHeader("{ \"alg\": \"PS256\" }");
		cond.execute(env);
	}

	@Test
	public void symmetricAlg_succeeds() {
		putHeader("{ \"alg\": \"HS256\" }");
		cond.execute(env);
	}

	@Test
	public void edDsaAlg_succeeds() {
		putHeader("{ \"alg\": \"EdDSA\" }");
		cond.execute(env);
	}
}
