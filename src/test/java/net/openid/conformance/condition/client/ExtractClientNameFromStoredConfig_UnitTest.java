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
public class ExtractClientNameFromStoredConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractClientNameFromStoredConfig cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new ExtractClientNameFromStoredConfig();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noClientNameInConfig() {
		env.putObject("original_client_config", new JsonObject());

		cond.execute(env);

		assertThat(env.getObject("client_name")).isNull();
	}

	/**
	 * Test method for {@link GetDynamicClientConfiguration#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_ClientNameInConfig() {
		JsonObject config = new JsonParser().parse("{" +
			"\"client_name\":\"foo\"" +
			"}").getAsJsonObject();
		env.putObject("original_client_config", config);

		cond.execute(env);

		assertThat(env.getString("client_name")).isNotNull();
		assertThat(env.getString("client_name")).isEqualTo("foo");
	}
}
