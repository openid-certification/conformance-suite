package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureClientCertificateCNMatchesClientId_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureClientCertificateCNMatchesClientId cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureClientCertificateCNMatchesClientId();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject client = new JsonParser().parse("{" +
			"\"client_id\":\"CN=example.org\"" +
			"}").getAsJsonObject();

		env.putObject("client", client);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.EnsureClientCertificateCNMatchesClientId#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("client_certificate", new JsonParser().parse("{\"subject\":{\"dn\":\"CN=example.org\"}}").getAsJsonObject());

		cond.execute(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.EnsureClientCertificateCNMatchesClientId#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongName() {

		env.putObject("client_certificate", new JsonParser().parse("{\"subject\":{\"dn\":\"CN=invalid.org\"}}").getAsJsonObject());

		cond.execute(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.EnsureClientCertificateCNMatchesClientId#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingName() {

		env.putObject("client_certificate", new JsonParser().parse("{\"subject\":{\"dn\":\"\"}}").getAsJsonObject());

		cond.execute(env);

	}

}
