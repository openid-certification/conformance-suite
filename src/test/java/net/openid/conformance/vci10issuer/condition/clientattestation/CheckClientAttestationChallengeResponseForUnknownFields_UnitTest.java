package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractVciUnitTest;
import net.openid.conformance.vci10issuer.condition.VCIValidateCredentialNonceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CheckClientAttestationChallengeResponseForUnknownFields_UnitTest extends AbstractVciUnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckClientAttestationChallengeResponseForUnknownFields cond;

	private Environment env;

	@BeforeEach
	void setUp() {
		cond = new CheckClientAttestationChallengeResponseForUnknownFields();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	void acceptsAttestationChallenge() {
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void acceptsCombinedResponseFromSharedChallengeAndNonceEndpoint() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/challenge");
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge", "c_nonce": "credential-nonce"}
			""");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	void reportsCredentialNonceWhenEndpointsDiffer() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/nonce");
		assertCredentialNonceIsUnknown();
	}

	@Test
	void reportsCredentialNonceWithoutServerMetadata() {
		env.putString("vci", "credential_issuer_metadata.nonce_endpoint", "https://issuer.example.com/challenge");
		assertCredentialNonceIsUnknown();
	}

	@Test
	void reportsCredentialNonceWithoutChallengeEndpointMetadata() {
		env.putObject("server", new JsonObject());
		env.putString("vci", "credential_issuer_metadata.nonce_endpoint", "https://issuer.example.com/challenge");
		assertCredentialNonceIsUnknown();
	}

	@Test
	void reportsCredentialNonceWithoutCredentialIssuerMetadata() {
		env.putString("server", "challenge_endpoint", "https://issuer.example.com/challenge");
		assertCredentialNonceIsUnknown();
	}

	@Test
	void reportsCredentialNonceWithoutNonceEndpointMetadata() {
		env.putString("server", "challenge_endpoint", "https://issuer.example.com/challenge");
		env.putObject("vci", "credential_issuer_metadata", new JsonObject());
		assertCredentialNonceIsUnknown();
	}

	@Test
	void reportsCredentialNonceWithoutEitherEndpointMetadata() {
		assertCredentialNonceIsUnknown();
	}

	@Test
	void reportsOnlyUnknownFieldsInCombinedResponse() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/challenge");
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge", "c_nonce": "credential-nonce", "unexpected": true}
			""");

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertEquals(Set.of("unexpected"), data.get("unknown_fields"));
	}

	@Test
	void reportsUnknownFieldsWithoutCredentialNonce() {
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge", "unexpected": true}
			""");

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertEquals(Set.of("unexpected"), data.get("unknown_fields"));
	}

	@Test
	void combinedResponsePassesBothChallengeAndNonceValidators() {
		putEndpoints("https://issuer.example.com/challenge", "https://issuer.example.com/challenge");
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge", "c_nonce": "credential-nonce"}
			""");
		ValidateClientAttestationChallengeResponse challengeCond = new ValidateClientAttestationChallengeResponse();
		challengeCond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		VCIValidateCredentialNonceResponse nonceCond = new VCIValidateCredentialNonceResponse();
		nonceCond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);

		assertDoesNotThrow(() -> challengeCond.execute(env));
		assertDoesNotThrow(() -> nonceCond.execute(env));
		assertEquals("client-attestation-challenge", env.getString("vci", "attestation_challenge"));
		assertEquals("credential-nonce", env.getString("vci", "c_nonce"));
	}

	private void assertCredentialNonceIsUnknown() {
		putResponse("""
			{"attestation_challenge": "client-attestation-challenge", "c_nonce": "credential-nonce"}
			""");

		Map<String, Object> data = assertValidationError(cond, env, eventLog);
		assertEquals(Set.of("c_nonce"), data.get("unknown_fields"));
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
