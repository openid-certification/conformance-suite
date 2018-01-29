package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
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
	@Before
	public void setUp() throws Exception {
		
		cond = new GetStaticClientConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		client = new JsonParser().parse("{"
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
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {
		
		env.put("config", goodConfig);
		
		cond.evaluate(env);
		
		assertThat(env.get("client")).isEqualTo(client);
		assertThat(env.getString("client_id")).isEqualTo("client");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_clientNotObject() {
		
		env.put("config", badConfig_notObject);
		
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_clientMissing() {
		
		env.put("config", badConfig_clientMissing);
		
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_configMissing() {
		
		cond.evaluate(env);
	}
}
