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

	private static final String useDpopNonceErrorBody = "{\"error\":\"use_dpop_nonce\"}";

	private static final String successBody = "{\"access_token\":\"at\",\"token_type\":\"DPoP\"}";

	private CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse cond;

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
			service("dpop-success.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(successBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "rotated-success-nonce")),
			service("dpop-success-no-nonce.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(successBody)
					.header("Content-Type", "application/json"))));
		hoverfly.resetJournal();

		cond = new CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testStoresDpopNonceFromError() {
		env.putString("server", "token_endpoint", "https://dpop-nonce.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("the-nonce");
	}

	@Test
	public void testHarvestsDpopNonceFromSuccessResponse() {
		// RFC 9449 §8.2: the AS may rotate the DPoP nonce on every response. Some ASes treat
		// each nonce as single-use — reusing one returns invalid_dpop_proof with no recovery.
		// Previously, this wrapper only harvested the nonce on use_dpop_nonce 400 errors,
		// so a fresh nonce supplied with a successful 2xx response was dropped on the floor
		// and the next request reused the stale nonce — observed cross-client in the VCI
		// issuer-happy-flow-multiple-clients test on a single-use-nonce AS.
		env.putString("server", "token_endpoint", "https://dpop-success.example.com/token");
		env.putString("authorization_server_dpop_nonce", "old-nonce");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("rotated-success-nonce");
		// The use_dpop_nonce error flag must not be set on a 2xx — the retry loop would
		// otherwise re-call the endpoint unnecessarily.
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testLeavesDpopNonceUnchangedOnSuccessResponseWithoutHeader() {
		env.putString("server", "token_endpoint", "https://dpop-success-no-nonce.example.com/token");
		env.putString("authorization_server_dpop_nonce", "previous-nonce");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("previous-nonce");
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
	}
}
