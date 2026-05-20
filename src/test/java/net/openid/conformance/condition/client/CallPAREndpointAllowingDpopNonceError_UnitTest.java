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

	private static final String useDpopNonceErrorBody = "{\"error\":\"use_dpop_nonce\"}";

	private static final String successBody = "{\"request_uri\":\"urn:ietf:params:oauth:request_uri:abc\",\"expires_in\":60}";

	private CallPAREndpointAllowingDpopNonceError cond;

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
			service("dpop-success.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(successBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "rotated-success-nonce"))));
		hoverfly.resetJournal();

		cond = new CallPAREndpointAllowingDpopNonceError();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testStoresDpopNonceFromError() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-nonce.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isEqualTo("the-nonce");
		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("the-nonce");
	}

	@Test
	public void testHarvestsDpopNonceFromSuccessResponse() {
		// RFC 9449 §8.2: rotate per response; some ASes treat each nonce as single-use, so
		// a fresh nonce returned with a 2xx must be harvested for the next call. Previously
		// the wrapper only harvested on use_dpop_nonce 400s; this is the regression test for
		// the multi-client carry-over described in the VCI issuer-happy-flow-multiple-clients
		// failure report.
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-success.example.com/par");
		env.putString("authorization_server_dpop_nonce", "old-nonce");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("rotated-success-nonce");
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
	}
}
