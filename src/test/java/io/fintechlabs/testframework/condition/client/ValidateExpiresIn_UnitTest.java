package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateExpiresIn_UnitTest {
	
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodInteger;   
	private JsonObject badStringNumeric; 
	private JsonObject badNonPrimitive;
	private JsonObject badStringAlpha;
	
	private ValidateExpiresIn cond;
	
	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateExpiresIn("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodInteger = new JsonParser().parse("{\"expires_in\":3600}").getAsJsonObject();
		badStringNumeric = new JsonParser().parse("{\"expires_in\":\"3600\"}").getAsJsonObject();
		badNonPrimitive = new JsonParser().parse("{\"expires_in\":[1,2,3]}").getAsJsonObject();
		badStringAlpha = new JsonParser().parse("{\"expires_in\":\"fish\"}").getAsJsonObject();
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateExpiresIn#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void ValidateExpiresIn_GoodInteger() {
		
		env.put("expires_in", goodInteger);
		
		cond.evaluate(env);
	}
	
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateExpiresIn#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test (expected = ConditionError.class)
	public void ValidateExpiresIn_BadStringNumeric() {
		
		env.put("expires_in", badStringNumeric);
		
		cond.evaluate(env);
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateExpiresIn#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test (expected = ConditionError.class)
	public void ValidateExpiresIn_BadNonPrimitive() {
		
		env.put("expires_in", badNonPrimitive);
		
		cond.evaluate(env);
	}	
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateExpiresIn#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test (expected = ConditionError.class)
	public void ValidateExpiresIn_BadStringAlpha() {
		
		env.put("expires_in", badStringAlpha);
		
		cond.evaluate(env);
	}
	
	
}
