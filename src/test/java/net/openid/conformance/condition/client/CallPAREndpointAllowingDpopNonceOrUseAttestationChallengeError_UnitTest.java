package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.dsl.HoverflyDsl;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@ExtendWith(HoverflyExtension.class)
public class CallPAREndpointAllowingDpopNonceOrUseAttestationChallengeError_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"response_type\":\"code\","
		+ "\"client_id\":\"test-client\""
		+ "}").getAsJsonObject();

	private static final String useDpopNonceErrorBody = "{\"error\":\"use_dpop_nonce\"}";
	private static final String useAttestationChallengeErrorBody = "{\"error\":\"use_attestation_challenge\"}";

	private static final String successBody = "{\"request_uri\":\"urn:ietf:params:oauth:request_uri:abc\",\"expires_in\":60}";

	private CallPAREndpointAllowingDpopNonceOrUseAttestationChallengeError cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("dpop-nonce.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useDpopNonceErrorBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "the-nonce")),
			service("attestation-challenge.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useAttestationChallengeErrorBody)
					.header("Content-Type", "application/json")
					.header("OAuth-Client-Attestation-Challenge", "the-challenge")),
			service("attestation-challenge-no-header.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useAttestationChallengeErrorBody)
					.header("Content-Type", "application/json")),
			service("success.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(successBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "rotated-success-nonce"))));
		hoverfly.resetJournal();

		cond = new CallPAREndpointAllowingDpopNonceOrUseAttestationChallengeError();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
	}

	@Test
	public void testStoresDpopNonceFromError() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-nonce.example.com/par");

		cond.execute(env);

		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("the-nonce");
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}

	@Test
	public void testFlagsUseAttestationChallengeFromError() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");

		cond.execute(env);

		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testFailsWhenUseAttestationChallengeErrorMissesChallengeHeader() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge-no-header.example.com/par");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * Simulates the DPoP+client_attestation retry loop in {@link
	 * net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule#callParEndpointAndStopOnFailure}
	 * for a server whose checks surface one error per round trip: attempt 1 returns use_dpop_nonce,
	 * attempt 2 (fresh nonce used) returns use_attestation_challenge, attempt 3 (fresh challenge used)
	 * succeeds. Three attempts are needed, which is why the module loop allows MAX_RETRY + 1 attempts
	 * for client_attestation — with only two, the loop would exit after the second 400 and downstream
	 * validation would fail a compliant flow.
	 */
	@Test
	public void testDpopNonceThenAttestationChallengeThenSuccess() {
		// attempt 1: use_dpop_nonce
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-nonce.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();

		// attempt 2: nonce accepted, now use_attestation_challenge
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");

		// attempt 3: both fresh values used, success — both flags must be clear so the loop exits
		env.putString("server", "pushed_authorization_request_endpoint", "https://success.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * Reverse ordering of {@link #testDpopNonceThenAttestationChallengeThenSuccess}: an AS may check
	 * client authentication before the DPoP proof, so use_attestation_challenge can arrive first.
	 */
	@Test
	public void testAttestationChallengeThenDpopNonceThenSuccess() {
		// attempt 1: use_attestation_challenge
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();

		// attempt 2: challenge accepted, now use_dpop_nonce
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-nonce.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");

		// attempt 3: both fresh values used, success — both flags must be clear so the loop exits
		env.putString("server", "pushed_authorization_request_endpoint", "https://success.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}
}
