package net.openid.conformance.condition.common;

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
		cond = new EnsureTLS12();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link EnsureTLS12#evaluate(Environment)}.
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

		env.putObject("tls", tls);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("tls", "testHost");
		verify(env, atLeastOnce()).getInteger("tls", "testPort");
	}

	/**
	 * Test method for {@link EnsureTLS12#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_notTls() {

		JsonObject tls = new JsonParser().parse("{"
			+ "\"testHost\":\"example.com\","
			+ "\"testPort\":80"
			+ "}").getAsJsonObject();

		env.putObject("tls", tls);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureTLS12#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingHost() {

		JsonObject tls = new JsonParser().parse("{"
			+ "\"testPort\":443"
			+ "}").getAsJsonObject();

		env.putObject("tls", tls);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureTLS12#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingPort() {

		JsonObject tls = new JsonParser().parse("{"
			+ "\"testHost\":\"example.com\""
			+ "}").getAsJsonObject();

		env.putObject("tls", tls);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureTLS12#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingConfig() {

		cond.execute(env);
	}
}
