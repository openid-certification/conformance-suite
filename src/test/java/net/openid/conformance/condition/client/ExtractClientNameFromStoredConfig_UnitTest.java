package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ExtractClientNameFromStoredConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractClientNameFromStoredConfig cond;

	/**
	 * @throws Exception
	 */
	@BeforeEach
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
	 * Test method for {@link StoreOriginalClientConfiguration#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_ClientNameInConfig() {
		JsonObject config = JsonParser.parseString("{" +
			"\"client_name\":\"foo\"" +
			"}").getAsJsonObject();
		env.putObject("original_client_config", config);

		cond.execute(env);

		assertThat(env.getString("client_name")).isNotNull();
		assertThat(env.getString("client_name")).isEqualTo("foo");
	}
}
