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
public class CallTokenEndpointAllowingDpopNonceOrUseAttestationChallengeErrorAndReturnFullResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"grant_type\":\"client_credentials\""
		+ "}").getAsJsonObject();

	private static final String useDpopNonceErrorBody = "{\"error\":\"use_dpop_nonce\"}";
	private static final String useAttestationChallengeErrorBody = "{\"error\":\"use_attestation_challenge\"}";

	private static final String successBody = "{\"access_token\":\"at\",\"token_type\":\"DPoP\"}";

	private CallTokenEndpointAllowingDpopNonceOrUseAttestationChallengeErrorAndReturnFullResponse cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("dpop-nonce.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useDpopNonceErrorBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "the-nonce")),
			service("attestation-challenge.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useAttestationChallengeErrorBody)
					.header("Content-Type", "application/json")
					.header("OAuth-Client-Attestation-Challenge", "the-challenge")),
			service("attestation-challenge-no-header.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useAttestationChallengeErrorBody)
					.header("Content-Type", "application/json")),
			service("success.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(successBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "rotated-success-nonce"))));
		hoverfly.resetJournal();

		cond = new CallTokenEndpointAllowingDpopNonceOrUseAttestationChallengeErrorAndReturnFullResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());
	}

	@Test
	public void testStoresDpopNonceFromError() {
		env.putString("server", "token_endpoint", "https://dpop-nonce.example.com/token");

		cond.execute(env);

		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("the-nonce");
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
	}

	@Test
	public void testFlagsUseAttestationChallengeFromError() {
		env.putString("server", "token_endpoint", "https://attestation-challenge.example.com/token");

		cond.execute(env);

		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testFailsWhenUseAttestationChallengeErrorMissesChallengeHeader() {
		env.putString("server", "token_endpoint", "https://attestation-challenge-no-header.example.com/token");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * Simulates the DPoP+client_attestation retry loop in {@link
	 * net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule#callSenderConstrainedTokenEndpoint}
	 * for a server whose checks surface one error per round trip: attempt 1 returns use_dpop_nonce,
	 * attempt 2 (fresh nonce used) returns use_attestation_challenge, attempt 3 (fresh challenge used)
	 * succeeds. Three attempts are needed, which is why the module loop allows MAX_RETRY + 1 attempts
	 * for client_attestation — with only two, the loop would exit after the second 400 and downstream
	 * 200-validation would fail a compliant flow.
	 */
	@Test
	public void testDpopNonceThenAttestationChallengeThenSuccess() {
		// attempt 1: use_dpop_nonce
		env.putString("server", "token_endpoint", "https://dpop-nonce.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();

		// attempt 2: nonce accepted, now use_attestation_challenge
		env.putString("server", "token_endpoint", "https://attestation-challenge.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");

		// attempt 3: both fresh values used, success — both flags must be clear so the loop exits
		env.putString("server", "token_endpoint", "https://success.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
		assertThat(env.getInteger("token_endpoint_response_http_status")).isEqualTo(200);
	}

	/**
	 * Reverse ordering of {@link #testDpopNonceThenAttestationChallengeThenSuccess}: an AS may check
	 * client authentication before the DPoP proof, so use_attestation_challenge can arrive first.
	 */
	@Test
	public void testAttestationChallengeThenDpopNonceThenSuccess() {
		// attempt 1: use_attestation_challenge
		env.putString("server", "token_endpoint", "https://attestation-challenge.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();

		// attempt 2: challenge accepted, now use_dpop_nonce
		env.putString("server", "token_endpoint", "https://dpop-nonce.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");

		// attempt 3: both fresh values used, success — both flags must be clear so the loop exits
		env.putString("server", "token_endpoint", "https://success.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
		assertThat(env.getInteger("token_endpoint_response_http_status")).isEqualTo(200);
	}
}
