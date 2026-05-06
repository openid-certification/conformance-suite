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
public class CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"grant_type\":\"client_credentials\""
		+ "}").getAsJsonObject();

	private static final JsonObject goodResponse = JsonParser.parseString("{"
		+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
		+ "\"token_type\":\"example\","
		+ "\"expires_in\":3600"
		+ "}").getAsJsonObject();

	private static final String useDpopNonceErrorBody = "{\"error\":\"use_dpop_nonce\"}";

	private CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("with-challenge.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(goodResponse.toString())
					.header("Content-Type", "application/json")
					.header("OAuth-Client-Attestation-Challenge", "fresh-challenge-1")),
			service("no-challenge.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(goodResponse.toString())
					.header("Content-Type", "application/json")),
			service("dpop-and-challenge.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useDpopNonceErrorBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "the-nonce")
					.header("OAuth-Client-Attestation-Challenge", "fresh-challenge-2")),
			service("lowercase-challenge.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(goodResponse.toString())
					.header("Content-Type", "application/json")
					.header("oauth-client-attestation-challenge", "lowercase-challenge"))));
		hoverfly.resetJournal();

		cond = new CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testHarvestsHeaderOnSuccess() {
		env.putString("server", "token_endpoint", "https://with-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("fresh-challenge-1");
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testNoHarvestWhenHeaderAbsent() {
		env.putString("server", "token_endpoint", "https://no-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void testHarvestsAlongsideDpopNonceError() {
		env.putString("server", "token_endpoint", "https://dpop-and-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("fresh-challenge-2");
	}

	@Test
	public void testHarvestIsCaseInsensitive() {
		env.putString("server", "token_endpoint", "https://lowercase-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("lowercase-challenge");
	}
}
