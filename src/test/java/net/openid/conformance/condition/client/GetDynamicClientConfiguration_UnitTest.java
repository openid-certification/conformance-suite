package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
		cond = new GetDynamicClientConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noClientConfig() {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertThat(env.getObject("dynamic_client_registration_template")).isInstanceOf(JsonObject.class);
	}

	@Test
	public void testEvaluate_noClientNameInConfig() {
		JsonObject config = new JsonParser().parse("{" +
			"\"client\":{}" +
			"}").getAsJsonObject();
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getObject("dynamic_client_registration_template")).isInstanceOf(JsonObject.class);
		assertThat(env.getObject("dynamic_client_registration_template").get("client_name")).isNull();
		assertThat(env.getObject("client_name")).isNull();
	}

	/**
	 * Test method for {@link GetDynamicClientConfiguration#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_ClientNameInConfig() {
		JsonObject config = new JsonParser().parse("{" +
			"\"client\":{\"client_name\":\"foo\"}" +
			"}").getAsJsonObject();
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getObject("dynamic_client_registration_template")).isInstanceOf(JsonObject.class);
		assertThat(env.getObject("dynamic_client_registration_template").get("client_name")).isNotNull();
		assertThat(OIDFJSON.getString(env.getObject("dynamic_client_registration_template").get("client_name"))).isEqualTo("foo");
		assertThat(env.getString("client_name")).isNotNull();
		assertThat(env.getString("client_name")).isEqualTo("foo");
	}
}
