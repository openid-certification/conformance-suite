package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CreateTokenRevocationRequest_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateTokenRevocationRequest cond;

	private JsonObject accessToken = new JsonParser().parse("{\"value\":\"2YotnFZFEjr1zCsicMWpAA\"}").getAsJsonObject();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CreateTokenRevocationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CreateTokenRevocationRequest#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate() {
		env.putObject("access_token", accessToken);
		cond.execute(env);

		String tokenVal = env.getString("revocation_endpoint_request_form_parameters", "token");
		assertThat(tokenVal).isNotEmpty();
		assertThat(tokenVal.equals(OIDFJSON.getString(accessToken.get("value"))));
	}
}
