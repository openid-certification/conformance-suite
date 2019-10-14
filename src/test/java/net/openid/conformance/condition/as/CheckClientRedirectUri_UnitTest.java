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
public class CheckClientRedirectUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private CheckClientRedirectUri cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckClientRedirectUri();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://localhost/foo\"]\n" +
			"}").getAsJsonObject();

	}

	@Test
	public void testEvaluate_httpLocalhost() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://localhost/foo\"]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_httpLocalIp() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://127.0.0.1/foo\"]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_httpLocalIpv6() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://::::1/foo\"]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_httpsNonLocalhost() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"https://example.com/foo\"]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nonHttp() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"nonhttp:/callback\"]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_httpNonLocalhost() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://example.com/foo\"]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_empty() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[]\n" +
			"}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_null() {

		client = new JsonParser().parse("{}").getAsJsonObject();

		env.putObject("client", client);

		cond.execute(env);
	}

}
