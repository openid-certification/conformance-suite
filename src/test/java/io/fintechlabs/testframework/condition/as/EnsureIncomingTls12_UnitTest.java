package io.fintechlabs.testframework.condition.as;

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
public class EnsureIncomingTls12_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIncomingTls12 cond;

	private JsonObject hasTls;
	private JsonObject wrongTls;
	private JsonObject missingTls;
	private JsonObject onlyTls;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureIncomingTls12("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\", \"X-Ssl-Cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		wrongTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.1\", \"X-Ssl-Cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		missingTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		onlyTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\"}"
			+ "}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.EnsureClientCertificateCNMatchesClientId#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("client_request", hasTls);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.X-Ssl-Protocol");

	}
	@Test(expected = ConditionError.class)
	public void testEvaluate_wrong() {

		env.putObject("client_request", wrongTls);

		cond.evaluate(env);

	}
	@Test(expected = ConditionError.class)
	public void testEvaluate_missing() {

		env.putObject("client_request", missingTls);

		cond.evaluate(env);

	}
	@Test
	public void testEvaluate_only() {

		env.putObject("client_request", onlyTls);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.X-Ssl-Protocol");

	}
}
