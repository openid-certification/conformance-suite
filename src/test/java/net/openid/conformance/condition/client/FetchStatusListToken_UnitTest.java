package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class FetchStatusListToken_UnitTest {

	private static final String DUMMY_JWT =
		"eyJhbGciOiJub25lIn0.eyJzdGF0dXNfbGlzdCI6eyJiaXRzIjoxLCJsc3QiOiJlTnJiQUFBQUNBQUUifX0.";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private TestableFetchStatusListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new TestableFetchStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject statusList = new JsonObject();
		statusList.addProperty("idx", 3);
		statusList.addProperty("uri", "https://issuer.example.com/statuslists/1");

		JsonObject status = new JsonObject();
		status.add("status_list", statusList);

		JsonObject claims = new JsonObject();
		claims.add("status", status);
		env.putObject("sdjwt", "credential.claims", claims);
	}

	@Test
	public void testEvaluate_skipsWhenNoStatusClaim() {
		env.putObject("status_list_token_endpoint_response", new JsonObject());
		env.putObject("status_list_token", new JsonObject());
		env.putInteger("status_list_idx", 9);
		env.putString("status_list_uri", "https://issuer.example.com/statuslists/stale");
		env.putObject("sdjwt", "credential.claims", new JsonObject());

		cond.execute(env);

		assertFalse(env.containsObject("status_list_token_endpoint_response"));
		assertFalse(env.containsObject("status_list_token"));
		assertEquals(null, env.getInteger("status_list_idx"));
		assertEquals(null, env.getString("status_list_uri"));
	}

	@Test
	public void testEvaluate_rejectsMissingStatusList() {
		JsonObject status = new JsonObject();
		JsonObject claims = new JsonObject();
		claims.add("status", status);
		env.putObject("sdjwt", "credential.claims", claims);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingIdx() {
		JsonObject statusList = new JsonObject();
		statusList.addProperty("uri", "https://issuer.example.com/statuslists/1");
		JsonObject status = new JsonObject();
		status.add("status_list", statusList);
		JsonObject claims = new JsonObject();
		claims.add("status", status);
		env.putObject("sdjwt", "credential.claims", claims);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsMissingUri() {
		JsonObject statusList = new JsonObject();
		statusList.addProperty("idx", 0);
		JsonObject status = new JsonObject();
		status.add("status_list", statusList);
		JsonObject claims = new JsonObject();
		claims.add("status", status);
		env.putObject("sdjwt", "credential.claims", claims);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_storesResponseAndTokenOnSuccess() {
		cond.setResponse(ResponseEntity.ok(DUMMY_JWT));

		cond.execute(env);

		assertTrue(env.containsObject("status_list_token_endpoint_response"));
		assertFalse(env.containsObject("status_list_token"));
		assertEquals("https://issuer.example.com/statuslists/1", env.getString("status_list_uri"));
		assertEquals(3, env.getInteger("status_list_idx").intValue());
	}

	@Test
	public void testEvaluate_storesResponseAndFailsOnNon2xx() {
		cond.setResponse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found"));

		assertThrows(ConditionError.class, () -> cond.execute(env));

		// The full response should still be stored so downstream content-type
		// checks can see what came back.
		assertTrue(env.containsObject("status_list_token_endpoint_response"));
	}

	private static class TestableFetchStatusListToken extends FetchStatusListToken {
		private ResponseEntity<String> response;

		void setResponse(ResponseEntity<String> response) {
			this.response = response;
		}

		@Override
		protected ResponseEntity<String> fetchStatusListToken(Environment env, String uri) {
			return response;
		}
	}
}
