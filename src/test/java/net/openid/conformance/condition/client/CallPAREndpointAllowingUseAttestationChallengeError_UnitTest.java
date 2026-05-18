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
public class CallPAREndpointAllowingUseAttestationChallengeError_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"response_type\":\"code\","
		+ "\"client_id\":\"test-client\""
		+ "}").getAsJsonObject();

	private static final String useAttestationChallengeErrorBody = "{\"error\":\"use_attestation_challenge\"}";
	private static final String happyBody = "{\"request_uri\":\"urn:ietf:params:oauth:request_uri:abc\",\"expires_in\":60}";

	private CallPAREndpointAllowingUseAttestationChallengeError cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
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
			service("happy.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(happyBody)
					.header("Content-Type", "application/json"))));
		hoverfly.resetJournal();

		cond = new CallPAREndpointAllowingUseAttestationChallengeError();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void flagsUseAttestationChallenge() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
	}

	@Test
	public void failsWhenChallengeHeaderIsMissing() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge-no-header.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		org.junit.jupiter.api.Assertions.assertThrows(
			net.openid.conformance.condition.ConditionError.class,
			() -> cond.execute(env));
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * Simulates the retry loop in the non-DPoP client_attestation branch of
	 * {@link net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule#callParEndpointAndStopOnFailure}:
	 * first call sets the flag, second (success) call clears it via evaluate().
	 */
	@Test
	public void retryClearsFlagOnSuccess() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
		cond.execute(env);
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error"))
			.isEqualTo("use_attestation_challenge");

		env.putString("server", "pushed_authorization_request_endpoint", "https://happy.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}
}
