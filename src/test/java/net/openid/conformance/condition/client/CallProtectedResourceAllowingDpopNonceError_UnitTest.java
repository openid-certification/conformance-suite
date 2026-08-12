package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class CallProtectedResourceAllowingDpopNonceError_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static final String USE_DPOP_NONCE_CHALLENGE =
		"DPoP error=\"use_dpop_nonce\", error_description=\"Resource server requires nonce in DPoP proof\"";

	private CallProtectedResourceAllowingDpopNonceError cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("rs-nonce.example.com")
				.get("/resource")
				.willReturn(HoverflyDsl.response()
					.status(401)
					.header("WWW-Authenticate", USE_DPOP_NONCE_CHALLENGE)
					.header("DPoP-Nonce", "rs-nonce")),
			service("rs-nonce-missing.example.com")
				.get("/resource")
				.willReturn(HoverflyDsl.response()
					.status(401)
					.header("WWW-Authenticate", USE_DPOP_NONCE_CHALLENGE)),
			service("rs-bad-nonce.example.com")
				.get("/resource")
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body("OK")
					.header("Content-Type", "text/plain")
					.header("DPoP-Nonce", "nonce with spaces")),
			service("rs-rotating-nonce.example.com")
				.get("/resource")
				.willReturn(HoverflyDsl.response()
					.status(200)
					.body("OK")
					.header("Content-Type", "text/plain")
					.header("DPoP-Nonce", "rotated-success-nonce"))));
		hoverfly.resetJournal();

		cond = new CallProtectedResourceAllowingDpopNonceError();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("resource", new JsonObject());
		env.putObjectFromJsonString("access_token", "{"
			+ "\"value\":\"FZFEjr1zCsicM\","
			+ "\"type\":\"DPoP\""
			+ "}");
	}

	@Test
	public void testStoresDpopNonceFromChallenge() {
		env.putString("protected_resource_url", "http://rs-nonce.example.com/resource");

		cond.execute(env);

		assertThat(env.getString("resource_server_dpop_nonce")).isEqualTo("rs-nonce");
		assertThat(env.getString("resource_endpoint_dpop_nonce_error")).isEqualTo("rs-nonce");
	}

	@Test
	public void testHarvestsDpopNonceFromSuccessResponse() {
		// RFC 9449 §9 applies §8.2 to resource servers too: a nonce supplied on a successful response
		// replaces the stored one for the next resource request. No retry is being requested, so the
		// resource_endpoint_dpop_nonce_error retry trigger must stay unset.
		env.putString("resource_server_dpop_nonce", "old-nonce");
		env.putString("protected_resource_url", "http://rs-rotating-nonce.example.com/resource");

		cond.execute(env);

		assertThat(env.getString("resource_server_dpop_nonce")).isEqualTo("rotated-success-nonce");
		assertThat(env.getString("resource_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testRejectsUseDpopNonceChallengeWithNoNonceSupplied() {
		// There is no nonce to retry with, so the violation must be reported against this response rather
		// than the retry loop silently stopping and a later condition failing on the 401.
		env.putString("protected_resource_url", "http://rs-nonce-missing.example.com/resource");

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("resource_endpoint_dpop_nonce_error")).isNull();
	}

	@Test
	public void testRejectsNonceWithCharactersOutsideNqchar() {
		// RFC9449 section 8.1 defines the nonce as 1*NQCHAR, which does not include a space. The header is
		// checked whatever the status code was, so the violation is attributed to the response carrying it.
		env.putString("protected_resource_url", "http://rs-bad-nonce.example.com/resource");

		assertThrows(ConditionError.class, () -> cond.execute(env));

		assertThat(env.getString("resource_server_dpop_nonce")).isNull();
	}
}
