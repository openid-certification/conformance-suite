package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.dsl.HoverflyDsl;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
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

@ExtendWith(MockitoExtension.class)
@ExtendWith(HoverflyExtension.class)
public class CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"grant_type\":\"client_credentials\""
		+ "}").getAsJsonObject();

	private static final String useAttestationChallengeErrorBody = "{\"error\":\"use_attestation_challenge\"}";
	private static final String happyBody = "{\"access_token\":\"abc\",\"token_type\":\"Bearer\"}";

	private CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
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
			service("happy.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(happyBody)
					.header("Content-Type", "application/json"))));
		hoverfly.resetJournal();

		cond = new CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void flagsUseAttestationChallenge() {
		env.putString("server", "token_endpoint", "https://attestation-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
	}

	@Test
	public void doesNotFlagOnHappyResponse() {
		env.putString("server", "token_endpoint", "https://happy.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
	}

	@Test
	public void failsWhenChallengeHeaderIsMissing() {
		env.putString("server", "token_endpoint", "https://attestation-challenge-no-header.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		org.junit.jupiter.api.Assertions.assertThrows(
			net.openid.conformance.condition.ConditionError.class,
			() -> cond.execute(env));
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * Simulates the retry loop in the non-DPoP client_attestation branch of
	 * {@link net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule#callSenderConstrainedTokenEndpoint}:
	 * first call sets the flag, second (success) call clears it via evaluate().
	 */
	@Test
	public void retryClearsFlagOnSuccess() {
		env.putString("server", "token_endpoint", "https://attestation-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());
		cond.execute(env);
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error"))
			.isEqualTo("use_attestation_challenge");

		env.putString("server", "token_endpoint", "https://happy.example.com/token");
		cond.execute(env);
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
		assertThat(env.getInteger("token_endpoint_response_http_status")).isEqualTo(200);
	}
}
