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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class GetStaticClientConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private JsonObject goodConfig;

	private JsonObject badConfig_notObject;

	private JsonObject badConfig_clientMissing;

	private GetStaticClientConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new GetStaticClientConfiguration();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		client = JsonParser.parseString("{"
			+ "\"client_id\":\"client\","
			+ "\"client_secret\":\"secret\""
			+ "}").getAsJsonObject();

		goodConfig = new JsonObject();
		goodConfig.add("client", client);

		badConfig_notObject = new JsonObject();
		badConfig_notObject.addProperty("client", "this is a string");

		badConfig_clientMissing = new JsonObject();
	}

	/**
	 * Test method for {@link GetStaticClientConfiguration#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("config", goodConfig);

		cond.execute(env);

		assertThat(env.getObject("client")).isEqualTo(client);
		assertThat(env.getString("client_id")).isEqualTo("client");
	}

	/**
	 * Test method for {@link GetStaticClientConfiguration#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_clientNotObject() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("config", badConfig_notObject);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link GetStaticClientConfiguration#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_clientMissing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("config", badConfig_clientMissing);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link GetStaticClientConfiguration#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_configMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);
		});
	}
}
