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

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class VCICheckCredentialNonceResponseForUnknownFields_UnitTest extends AbstractVciUnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCICheckCredentialNonceResponseForUnknownFields cond;

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new VCICheckCredentialNonceResponseForUnknownFields();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	void acceptsCredentialNonce() {
		putResponse("""
			{"c_nonce": "credential-nonce"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsCombinedResponseFromSharedChallengeAndNonceEndpoint() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/challenge");
		putResponse("""
			{"c_nonce": "credential-nonce", "attestation_challenge": "client-attestation-challenge"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void reportsAttestationChallengeWhenEndpointsDiffer() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/nonce");
		assertAttestationChallengeIsUnknown();
	}

	@Test
	void reportsAttestationChallengeWithoutServerMetadata() {
		env.putString("vci", "credential_issuer_metadata.nonce_endpoint", "https://issuer.example.com/challenge");
		assertAttestationChallengeIsUnknown();
	}

	@Test
	void reportsAttestationChallengeWithoutCredentialIssuerMetadata() {
		env.putString("server", "challenge_endpoint", "https://issuer.example.com/challenge");
		assertAttestationChallengeIsUnknown();
	}

	@Test
	void reportsAttestationChallengeWithoutEitherEndpointMetadata() {
		assertAttestationChallengeIsUnknown();
	}

	@Test
	void reportsOnlyUnknownFieldsInCombinedResponse() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/challenge");
		putResponse("""
			{"c_nonce": "credential-nonce", "attestation_challenge": "client-attestation-challenge", "unexpected": true}
			""");

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertEquals(Set.of("unexpected"), data.get("unknown_fields"));
	}

	@Test
	void reportsUnknownFieldsWithoutAttestationChallenge() {
		putResponse("""
			{"c_nonce": "credential-nonce", "unexpected": true}
			""");

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertEquals(Set.of("unexpected"), data.get("unknown_fields"));
	}

	private void assertAttestationChallengeIsUnknown() {
		putResponse("""
			{"c_nonce": "credential-nonce", "attestation_challenge": "client-attestation-challenge"}
			""");

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertEquals(Set.of("attestation_challenge"), data.get("unknown_fields"));
	}

	private void putEndpoints(String challengeEndpoint, String nonceEndpoint) {
		env.putString("server", "challenge_endpoint", challengeEndpoint);
		env.putString("vci", "credential_issuer_metadata.nonce_endpoint", nonceEndpoint);
	}

	private void putResponse(String body) {
		JsonObject response = new JsonObject();
		response.addProperty("body", body);
		env.putObject("endpoint_response", response);
	}
}
