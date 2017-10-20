package io.fintechlabs.testframework.condition;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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
public class EnsureTls12_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	private EnsureTls12 cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new EnsureTls12("UNIT-TEST", eventLog, false);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTls12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		
		/*
		 * Sadly, SSLSocket doesn't like connecting to Hoverfly.
		 * We'll go out to the actual internet for this test, but if this proves
		 * unreliable, we'll need to spin up a local server (or disable the test).
		 */
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testHost\":\"example.com\","
				+ "\"testPort\":443"
				+ "}").getAsJsonObject();
		
		JsonObject config = new JsonObject();
		config.add("tls", tls);
		
		env.put("config", config);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("config", "tls.testHost");
		verify(env, atLeastOnce()).getInteger("config", "tls.testPort");
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTls12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_notTls() {
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testHost\":\"example.com\","
				+ "\"testPort\":80"
				+ "}").getAsJsonObject();
		
		JsonObject config = new JsonObject();
		config.add("tls", tls);
		
		env.put("config", config);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTls12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingHost() {
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testPort\":443"
				+ "}").getAsJsonObject();
		
		JsonObject config = new JsonObject();
		config.add("tls", tls);
		
		env.put("config", config);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTls12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingPort() {
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testHost\":\"example.com\""
				+ "}").getAsJsonObject();
		
		JsonObject config = new JsonObject();
		config.add("tls", tls);
		
		env.put("config", config);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTls12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingConfig() {
		
		cond.evaluate(env);
	}
}
