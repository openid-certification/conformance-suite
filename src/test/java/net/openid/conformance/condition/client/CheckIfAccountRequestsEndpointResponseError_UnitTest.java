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
public class CheckIfAccountRequestsEndpointResponseError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject successParams;

	private JsonObject errorParams;

	private CheckIfAccountRequestsEndpointResponseError cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckIfAccountRequestsEndpointResponseError();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		successParams = new JsonObject();

		errorParams = JsonParser.parseString("{\"error\":\"access_denied\"}").getAsJsonObject();

	}

	/**
	 * Test method for {@link CheckIfAccountRequestsEndpointResponseError#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("account_requests_endpoint_response", successParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("account_requests_endpoint_response", "error");
	}

	/**
	 * Test method for {@link CheckIfAccountRequestsEndpointResponseError#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_error() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("account_requests_endpoint_response", errorParams);

			cond.execute(env);

		});

	}

}
