package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CheckForRefreshTokenValue_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodResponse;

	private JsonObject badResponse;

	private CheckForRefreshTokenValue cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForRefreshTokenValue();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodResponse = JsonParser.parseString("{"
			+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
			+ "\"token_type\":\"example\","
			+ "\"expires_in\":3600,"
			+ "\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\","
			+ "\"example_parameter\":\"example_value\""
			+ "}").getAsJsonObject();

		badResponse = JsonParser.parseString("{"
			+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
			+ "\"token_type\":\"example\","
			+ "\"expires_in\":3600,"
			+ "\"example_parameter\":\"example_value\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link CheckForRefreshTokenValue#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_response", goodResponse);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "refresh_token");
	}

	/**
	 * Test method for {@link CheckForRefreshTokenValue#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("token_endpoint_response", badResponse);

			cond.execute(env);
		});
	}
}
