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
public class CheckIfAuthorizationEndpointError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject errorParams;

	private JsonObject successParams;

	private CheckIfAuthorizationEndpointError cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckIfAuthorizationEndpointError();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		successParams = JsonParser.parseString("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		errorParams = JsonParser.parseString("{"
			+ "\"error\":\"access_denied\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link CheckIfAuthorizationEndpointError#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("authorization_endpoint_response", successParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("authorization_endpoint_response", "error");
	}

	/**
	 * Test method for {@link CheckIfAuthorizationEndpointError#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_error() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("authorization_endpoint_response", errorParams);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("authorization_endpoint_response", "error");
		});
	}
}
