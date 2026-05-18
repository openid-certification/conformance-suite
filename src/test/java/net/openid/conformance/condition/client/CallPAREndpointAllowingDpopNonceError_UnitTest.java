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
public class CallPAREndpointAllowingDpopNonceError_UnitTest {

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
					.header("DPoP-Nonce", "rotated-success-nonce")),
			service("dpop-success-bad-nonce.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(successBody)
					.header("Content-Type", "application/json")
					.header("DPoP-Nonce", "nonce with spaces")),
			service("dpop-nonce-error-without-nonce.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(400)
					.body(useDpopNonceErrorBody)
					.header("Content-Type", "application/json")),
			service("dpop-success-no-nonce.example.com")
				.post("/par")
				.anyBody()
				.willReturn(HoverflyDsl.response()
					.status(201)
					.body(successBody)
					.header("Content-Type", "application/json")),
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
					.header("Content-Type", "application/json"))));
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
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}

	@Test
	public void testFlagsUseAttestationChallengeFromError() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isEqualTo("use_attestation_challenge");
		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testFailsWhenUseAttestationChallengeErrorMissesChallengeHeader() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge-no-header.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
	}

	/**
	 * Simulates the retry loop in
	 * {@link net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule#callParEndpointAndStopOnFailure}:
	 * the first call returns 400 use_attestation_challenge (flag set), and a follow-up call to a
	 * success endpoint clears the flag via evaluate()'s removeNativeValue.
	 */
	@Test
	public void testRetryClearsUseAttestationChallengeFlagOnSuccess() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://attestation-challenge.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());
		cond.execute(env);
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error"))
			.isEqualTo("use_attestation_challenge");

		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-success-no-nonce.example.com/par");
		cond.execute(env);
		assertThat(env.getString("par_endpoint_use_attestation_challenge_error")).isNull();
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

	@Test
	public void testRejectsNonceWithCharactersOutsideNqchar() {
		// RFC9449 section 8.1 defines the nonce as 1*NQCHAR, which does not include a space. Harvesting the
		// value silently would leave the violation to surface later as an unrelated proof-construction failure.
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-success-bad-nonce.example.com/par");
		env.putString("authorization_server_dpop_nonce", "previous-nonce");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("authorization_server_dpop_nonce")).isEqualTo("previous-nonce");
	}

	@Test
	public void testRejectsUseDpopNonceErrorWithNoNonceSupplied() {
		// There is no nonce to retry with, so this must be reported rather than throwing a NullPointerException
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-nonce-error-without-nonce.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("par_endpoint_dpop_nonce_error")).isNull();
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
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-success.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		// in the message, so it is visible without expanding the entry; the nonce itself is not repeated
		// here because the response headers are logged with the response
		assertThat(parsedResponseLogMessage())
			.isEqualTo("Parsed pushed authorization request endpoint response - DPoP nonce supplied");
	}

	@Test
	public void testReportsTheAbsenceOfANonceOnTheParsedResponseEntry() {
		env.putString("server", "pushed_authorization_request_endpoint", "https://dpop-success-no-nonce.example.com/par");
		env.putObject("pushed_authorization_request_form_parameters", requestParameters);
		env.putObject("pushed_authorization_request_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		assertThat(parsedResponseLogMessage())
			.isEqualTo("Parsed pushed authorization request endpoint response - no DPoP nonce supplied");
	}
}
