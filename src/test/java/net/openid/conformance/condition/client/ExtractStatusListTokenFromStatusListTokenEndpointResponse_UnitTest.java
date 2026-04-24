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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ExtractStatusListTokenFromStatusListTokenEndpointResponse_UnitTest {

	private static final String DUMMY_JWT =
		"eyJhbGciOiJub25lIn0.eyJzdGF0dXNfbGlzdCI6eyJiaXRzIjoxLCJsc3QiOiJlTnJiQUFBQUNBQUUifX0.";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractStatusListTokenFromStatusListTokenEndpointResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractStatusListTokenFromStatusListTokenEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_skipsWhenNoResponse() {
		cond.execute(env);

		assertFalse(env.containsObject("status_list_token"));
	}

	@Test
	public void testEvaluate_parsesStatusListToken() {
		putResponseBody(DUMMY_JWT);

		cond.execute(env);

		assertTrue(env.containsObject("status_list_token"));
		assertEquals(DUMMY_JWT, env.getString("status_list_token", "value"));
	}

	@Test
	public void testEvaluate_rejectsMissingResponseBody() {
		env.putObject("status_list_token_endpoint_response", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsNonJwtResponseBody() {
		putResponseBody("not-a-jwt");

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertFalse(env.containsObject("status_list_token"));
	}

	private void putResponseBody(String jwt) {
		JsonObject response = new JsonObject();
		response.addProperty("body", jwt);
		env.putObject("status_list_token_endpoint_response", response);
	}
}
