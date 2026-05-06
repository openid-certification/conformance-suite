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
public class CallPAREndpointAllowingDpopNonceError_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"response_type\":\"code\","
		+ "\"client_id\":\"test-client\""
		+ "}").getAsJsonObject();

	private static final JsonObject goodResponse = JsonParser.parseString("{"
		+ "\"request_uri\":\"urn:ietf:params:oauth:request_uri:abc\","
		+ "\"expires_in\":300"
		+ "}").getAsJsonObject();

	private static final String useDpopNonceErrorBody = "{\"error\":\"use_dpop_nonce\"}";

	private CallPAREndpointAllowingDpopNonceError cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("with-challenge.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(goodResponse.toString())
					.header("Content-Type", "application/json")
					.header("OAuth-Client-Attestation-Challenge", "fresh-challenge-1")),
			service("no-challenge.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(goodResponse.toString())
					.header("Content-Type", "application/json")),
			service("dpop-and-challenge.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useDpopNonceErrorBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "the-nonce")
					.header("OAuth-Client-Attestation-Challenge", "fresh-challenge-2")),
			service("lowercase-challenge.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(goodResponse.toString())
					.header("Content-Type", "application/json")
					.header("oauth-client-attestation-challenge", "lowercase-challenge"))));
		hoverfly.resetJournal();

		cond = new CallPAREndpointAllowingDpopNonceError();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void prepareEnv(String parEndpointUrl) {
		env.putString("server", "pushed_authorization_request_endpoint", parEndpointUrl);
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
	}

	@Test
	public void testHarvestsHeaderOnSuccess() {
		prepareEnv("https://with-challenge.example.com/par");

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("fresh-challenge-1");
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testNoHarvestWhenHeaderAbsent() {
		prepareEnv("https://no-challenge.example.com/par");

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isNull();
	}

	@Test
	public void testHarvestsAlongsideDpopNonceError() {
		prepareEnv("https://dpop-and-challenge.example.com/par");

		cond.execute(env);

		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("fresh-challenge-2");
	}

	@Test
	public void testHarvestIsCaseInsensitive() {
		prepareEnv("https://lowercase-challenge.example.com/par");

		cond.execute(env);

		assertThat(env.getString("vci", "attestation_challenge")).isEqualTo("lowercase-challenge");
	}
}
