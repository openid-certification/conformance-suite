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
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
	private static final String useAttestationChallengeErrorBody = "{\"error\":\"use_attestation_challenge\"}";

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
					.header("Content-Type", "application/json")),
			service("dpop-success-bad-nonce.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body(successBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "nonce with spaces")),
			service("dpop-nonce-error-without-nonce.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useDpopNonceErrorBody)
					.header("Content-Type", "application/json")),
			service("attestation-challenge.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useAttestationChallengeErrorBody)
					.header("Content-Type", "application/json")
					.header("OAuth-Client-Attestation-Challenge", "the-challenge"))));
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
		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * use_attestation_challenge is an attestation-specific error, so the plain DPoP-nonce wrapper —
	 * used for private_key_jwt/mtls (+DPoP) flows — must NOT flag it as retryable: the module's retry
	 * loop would otherwise mask an AS wrongly returning it (once, then succeeding on retry) instead of
	 * the 400 surfacing as a test failure. Recognition lives only in
	 * {@link CallTokenEndpointAllowingDpopNonceOrUseAttestationChallengeErrorAndReturnFullResponse}.
	 */
	@Test
	public void testIgnoresUseAttestationChallengeErrorForNonAttestationAuth() {
		env.putString("server", "token_endpoint", "https://attestation-challenge.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("token_endpoint_use_attestation_challenge_error")).isNull();
		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
		// the 400 is left for downstream response validation to fail on
		assertThat(env.getInteger("token_endpoint_response_http_status")).isEqualTo(400);
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

	/**
	 * The number of log entries one call produces must not depend on whether the server happened to supply a
	 * DPoP nonce: the CI compare-results job diffs the sequence of log entries, so an entry that only appears
	 * when a nonce arrives shows up as a difference between two otherwise identical runs.
	 */
	private String parsedResponseLogMessage() {
		ArgumentCaptor<JsonObject> entries = ArgumentCaptor.forClass(JsonObject.class);
		// LoggingRequestInterceptor logs the request and the response, then the parsed-response entry
		verify(eventLog, times(3)).log(anyString(), entries.capture());
		// a map-payload entry would escape the captor above, and so would escape the count
		verify(eventLog, never()).log(anyString(), anyMap());

		return OIDFJSON.getString(entries.getAllValues().get(2).get("msg"));
	}

	@Test
	public void testReportsTheSuppliedNonceOnTheParsedResponseEntry() {
		env.putString("server", "token_endpoint", "https://dpop-success.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		// in the message, so it is visible without expanding the entry; the nonce itself is not repeated
		// here because the response headers are logged with the response
		assertThat(parsedResponseLogMessage()).isEqualTo("Parsed token endpoint response - DPoP nonce supplied");
	}

	@Test
	public void testReportsTheAbsenceOfANonceOnTheParsedResponseEntry() {
		env.putString("server", "token_endpoint", "https://dpop-success-no-nonce.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(parsedResponseLogMessage()).isEqualTo("Parsed token endpoint response - no DPoP nonce supplied");
	}

	@Test
	public void testRejectsNonceWithCharactersOutsideNqchar() {
		// RFC9449 section 8.1 defines the nonce as 1*NQCHAR, which does not include a space. Harvesting the
		// value silently would leave the violation to surface later as an unrelated proof-construction failure.
		env.putString("server", "token_endpoint", "https://dpop-success-bad-nonce.example.com/token");
		env.putString("authorization_server_dpop_nonce", "previous-nonce");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("previous-nonce");
	}

	@Test
	public void testRejectsUseDpopNonceErrorWithNoNonceSupplied() {
		// There is no nonce to retry with, so this must be reported rather than throwing a NullPointerException
		env.putString("server", "token_endpoint", "https://dpop-nonce-error-without-nonce.example.com/token");
		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("token_endpoint_dpop_nonce_error")).isNull();
	}
}
