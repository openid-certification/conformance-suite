package io.fintechlabs.testframework.condition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class GetStaticServerConfiguration_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	private JsonObject server;
	
	private JsonObject goodConfig;
	
	private JsonObject badConfig_notObject;
	
	private JsonObject badConfig_serverMissing;
	
	private GetStaticServerConfiguration cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new GetStaticServerConfiguration("UNIT-TEST", eventLog, false);
		
		server = new JsonParser().parse("{"
				+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
				+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
				+ "\"issuer\":\"ExampleApp\""
				+ "}").getAsJsonObject();
		
		goodConfig = new JsonObject();
		goodConfig.add("server", server);
		
		badConfig_notObject = new JsonObject();
		badConfig_notObject.addProperty("server", "this is a string");
		
		badConfig_serverMissing = new JsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetStaticServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {
		
		env.put("config", goodConfig);
		
		cond.evaluate(env);
		
		assertThat(env.get("server")).isEqualTo(server);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetStaticServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_serverNotObject() {
		
		env.put("config", badConfig_notObject);
		
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetStaticServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_serverMissing() {
		
		env.put("config", badConfig_serverMissing);
		
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetStaticServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_configMissing() {
		
		cond.evaluate(env);
	}
}
