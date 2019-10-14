package net.openid.conformance.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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

		cond = new ValidateExpiresIn();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodInteger = new JsonParser().parse("{\"expires_in\":3600}").getAsJsonObject();
		badStringNumeric = new JsonParser().parse("{\"expires_in\":\"3600\"}").getAsJsonObject();
		badNonPrimitive = new JsonParser().parse("{\"expires_in\":[1,2,3]}").getAsJsonObject();
		badStringAlpha = new JsonParser().parse("{\"expires_in\":\"fish\"}").getAsJsonObject();

	}

	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test
	public void ValidateExpiresIn_GoodInteger() {

		env.putObject("expires_in", goodInteger);

		cond.execute(env);
	}


	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test (expected = ConditionError.class)
	public void ValidateExpiresIn_BadStringNumeric() {

		env.putObject("expires_in", badStringNumeric);

		cond.execute(env);
	}

	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test (expected = ConditionError.class)
	public void ValidateExpiresIn_BadNonPrimitive() {

		env.putObject("expires_in", badNonPrimitive);

		cond.execute(env);
	}

	/**
	 * Test method for {@link ValidateExpiresIn#evaluate(Environment)}.
	 */
	@Test (expected = ConditionError.class)
	public void ValidateExpiresIn_BadStringAlpha() {

		env.putObject("expires_in", badStringAlpha);

		cond.execute(env);
	}


}
