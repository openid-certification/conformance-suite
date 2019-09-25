package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SetAuthorizationEndpointRequestResponseTypeFromConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetAuthorizationEndpointRequestResponseTypeFromConfig cond;

	@Before
	public void setUp() throws Exception {
		cond = new SetAuthorizationEndpointRequestResponseTypeFromConfig();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_code() {

		JsonObject config = new JsonObject();
		config.addProperty("response_type", "code");

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "response_type")).isEqualTo("code");
	}

	@Test
	public void testEvaluate_idToken() {

		JsonObject config = new JsonObject();
		config.addProperty("response_type", "id_token");

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "response_type")).isEqualTo("id_token");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missing() {

		JsonObject config = new JsonObject();

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject("config", config);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_empty() {

		JsonObject config = new JsonObject();
		config.addProperty("response_type", "");

		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject("config", config);

		cond.execute(env);
	}
}
