package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class AddRevocationUrlToServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject server;

	private String baseUrl;

	private AddRevocationUrlToServerConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new AddRevocationUrlToServerConfiguration();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		server = new JsonParser().parse("{\n" +
			"}").getAsJsonObject();

		baseUrl = "https://example.com/baseurl";

	}

	@Test
	public void testEvaluate() {

		env.putObject("server", server);
		env.putString("base_url", baseUrl);

		cond.execute(env);

		assertEquals(baseUrl + "/revoke", env.getString("server", "revocation_endpoint"));
	}

	@Test
	public void testEvaluate_trailingSlash() {

		env.putObject("server", server);
		env.putString("base_url", baseUrl + "/");

		cond.execute(env);

		assertEquals(baseUrl + "/revoke", env.getString("server", "revocation_endpoint"));
	}

}
