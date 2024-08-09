package net.openid.conformance.condition.common;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EnsureTLS12_WithFAPICiphers_UnitTest
{

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureTLS12WithFAPICiphers cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureTLS12WithFAPICiphers();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link EnsureTLS12WithFAPICiphers#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		/*
		 * Sadly, SSLSocket doesn't like connecting to Hoverfly.
		 * We'll go out to the actual internet for this test, but if this proves
		 * unreliable, we'll need to spin up a local server (or disable the test).
		 */

		JsonObject tls = JsonParser.parseString("{"
			+ "\"testHost\":\"example.com\","
			+ "\"testPort\":443"
			+ "}").getAsJsonObject();

		env.putObject("tls", tls);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("tls", "testHost");
		verify(env, atLeastOnce()).getInteger("tls", "testPort");
	}

	/**
	 * Test method for {@link EnsureTLS12WithFAPICiphers#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_notTls() {
		assertThrows(ConditionError.class, () -> {

			JsonObject tls = JsonParser.parseString("{"
				+ "\"testHost\":\"example.com\","
				+ "\"testPort\":80"
				+ "}").getAsJsonObject();

			env.putObject("tls", tls);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link EnsureTLS12WithFAPICiphers#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingHost() {
		assertThrows(ConditionError.class, () -> {

			JsonObject tls = JsonParser.parseString("{"
				+ "\"testPort\":443"
				+ "}").getAsJsonObject();

			env.putObject("tls", tls);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link EnsureTLS12WithFAPICiphers#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingPort() {
		assertThrows(ConditionError.class, () -> {

			JsonObject tls = JsonParser.parseString("{"
				+ "\"testHost\":\"example.com\""
				+ "}").getAsJsonObject();

			env.putObject("tls", tls);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link EnsureTLS12WithFAPICiphers#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingConfig() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);
		});
	}
}
