package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class GetDynamicClientConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private GetDynamicClientConfiguration cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new GetDynamicClientConfiguration("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noClientConfig() {
		env.put("config", new JsonObject());

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noClientNameInConfig() {
		JsonObject config = new JsonParser().parse("{" +
			"\"client\":{}" +
			"}").getAsJsonObject();
		env.put("config", config);

		cond.evaluate(env);

		assertThat(env.get("dynamic_client_registration_template")).isInstanceOf(JsonObject.class);
		assertThat(env.get("dynamic_client_registration_template").get("client_name")).isNull();
		assertThat(env.get("client_name")).isNull();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_ClientNameInConfig() {
		JsonObject config = new JsonParser().parse("{" +
			"\"client\":{\"client_name\":\"foo\"}" +
			"}").getAsJsonObject();
		env.put("config", config);

		cond.evaluate(env);

		assertThat(env.get("dynamic_client_registration_template")).isInstanceOf(JsonObject.class);
		assertThat(env.get("dynamic_client_registration_template").get("client_name")).isNotNull();
		assertThat(env.get("dynamic_client_registration_template").get("client_name").getAsString()).isEqualTo("foo");
		assertThat(env.getString("client_name")).isNotNull();
		assertThat(env.getString("client_name")).isEqualTo("foo");
	}
}
