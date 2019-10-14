package net.openid.conformance.condition.as;

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
public class CheckForClientCertificate_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject certificate;

	private CheckForClientCertificate cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckForClientCertificate();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		certificate = new JsonParser().parse("{\"subject\":{\"dn\":\"CN=example.org\"}}").getAsJsonObject();

	}

	/**
	 * Test method for {@link CheckForClientCertificate#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("client_certificate", certificate);

		cond.execute(env);

	}

	/**
	 * Test method for {@link CheckForClientCertificate#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.execute(env);

	}
}
