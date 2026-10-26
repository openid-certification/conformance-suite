package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class VCIValidateCredentialNonceResponse_UnitTest extends AbstractVciUnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIValidateCredentialNonceResponse cond;

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCIValidateCredentialNonceResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	void extractsCredentialNonce() {
		putResponse("""
			{"c_nonce": "credential-nonce"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals("credential-nonce", env.getString("vci", "c_nonce"));
	}

	@Test
	void extractsCredentialNonceFromCombinedChallengeAndNonceResponse() {
		putResponse("""
			{"c_nonce": "credential-nonce", "attestation_challenge": "client-attestation-challenge"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals("credential-nonce", env.getString("vci", "c_nonce"));
	}

	@Test
	void reportsMissingCredentialNonce() {
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge"}
			""");

		assertValidationError(cond, env, eventLog);
	}

	private void putResponse(String body) {
		JsonObject response = new JsonObject();
		response.addProperty("body", body);
		env.putObject("endpoint_response", response);
	}
}
