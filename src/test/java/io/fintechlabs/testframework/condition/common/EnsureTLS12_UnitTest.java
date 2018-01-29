package io.fintechlabs.testframework.condition.common;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureTLS12_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;
	
	private EnsureTLS12 cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new EnsureTLS12("UNIT-TEST", eventLog, ConditionResult.INFO);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.EnsureTLS12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
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
		
		env.put("tls", tls);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("tls", "testHost");
		verify(env, atLeastOnce()).getInteger("tls", "testPort");
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.EnsureTLS12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_notTls() {
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testHost\":\"example.com\","
				+ "\"testPort\":80"
				+ "}").getAsJsonObject();
		
		env.put("tls", tls);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.EnsureTLS12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingHost() {
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testPort\":443"
				+ "}").getAsJsonObject();
		
		env.put("tls", tls);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.EnsureTLS12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingPort() {
		
		JsonObject tls = new JsonParser().parse("{"
				+ "\"testHost\":\"example.com\""
				+ "}").getAsJsonObject();
		
		env.put("tls", tls);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.EnsureTLS12#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingConfig() {
		
		cond.evaluate(env);
	}
}
