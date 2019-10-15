package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class GetResourceEndpointConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject resourceConfig = new JsonParser().parse("{" +
		"\"resourceUrl\":\"https://example.com/\"" +
		"}").getAsJsonObject();

	private GetResourceEndpointConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new GetResourceEndpointConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link GetResourceEndpointConfiguration#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);
		env.putObject("config", config);

		cond.execute(env);

		verify(env, atLeastOnce()).getElementFromObject("config", "resource");
		assertThat(env.getObject("resource")).isEqualTo(resourceConfig);
	}

	/**
	 * Test method for {@link GetResourceEndpointConfiguration#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.putObject("config", new JsonObject());

		cond.execute(env);
	}

	/**
	 * Test method for {@link GetResourceEndpointConfiguration#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_configMissing() {

		cond.execute(env);
	}

}
