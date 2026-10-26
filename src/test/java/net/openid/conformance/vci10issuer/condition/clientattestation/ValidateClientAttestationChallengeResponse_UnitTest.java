package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ValidateClientAttestationChallengeResponse_UnitTest extends AbstractVciUnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateClientAttestationChallengeResponse cond;

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new ValidateClientAttestationChallengeResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	void extractsAttestationChallenge() {
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals("client-attestation-challenge", env.getString("vci", "attestation_challenge"));
	}

	@Test
	void extractsAttestationChallengeFromCombinedChallengeAndNonceResponse() {
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge", "c_nonce": "credential-nonce"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals("client-attestation-challenge", env.getString("vci", "attestation_challenge"));
	}

	@Test
	void reportsMissingAttestationChallenge() {
		putResponse("""
			{"c_nonce": "credential-nonce"}
			""");

		assertValidationError(cond, env, eventLog);
	}

	private void putResponse(String body) {
		JsonObject response = new JsonObject();
		response.addProperty("body", body);
		env.putObject("endpoint_response", response);
	}
}
