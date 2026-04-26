package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateKeyAttestationNonce_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateKeyAttestationNonce cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateKeyAttestationNonce();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putKeyAttestationClaims(JsonObject claims) {
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.add("claims", claims);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	private void putCNonce(String cNonce) {
		JsonObject credentialNonceResponse = new JsonObject();
		credentialNonceResponse.addProperty("c_nonce", cNonce);
		env.putObject("credential_nonce_response", credentialNonceResponse);
	}

	@Test
	public void passesWhenNoCNonceIssuedAndAttestationHasNoNonce() {
		putKeyAttestationClaims(new JsonObject());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWhenCNonceMatchesNonceInAttestation() {
		JsonObject claims = new JsonObject();
		claims.addProperty("nonce", "abc123");
		putKeyAttestationClaims(claims);
		putCNonce("abc123");

		assertDoesNotThrow(() -> cond.execute(env));
		assertNull(env.getObject("credential_nonce_response"), "c_nonce should be invalidated after successful match");
	}

	@Test
	public void failsWhenCNonceIssuedButAttestationHasNoNonce() {
		putKeyAttestationClaims(new JsonObject());
		putCNonce("abc123");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_nonce", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void failsWhenAttestationNonceMismatchesCNonce() {
		JsonObject claims = new JsonObject();
		claims.addProperty("nonce", "wrong");
		putKeyAttestationClaims(claims);
		putCNonce("abc123");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_nonce", env.getString("vci", "credential_error_response.body.error"));
	}
}
