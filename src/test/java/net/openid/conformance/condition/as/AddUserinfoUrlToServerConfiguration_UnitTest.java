package net.openid.conformance.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class AddUserinfoUrlToServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject server;

	private String baseUrl;

	private AddUserinfoUrlToServerConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new AddUserinfoUrlToServerConfiguration();

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

		assertEquals(baseUrl + "/userinfo", env.getString("server", "userinfo_endpoint"));
	}

	@Test
	public void testEvaluate_trailingSlash() {

		env.putObject("server", server);
		env.putString("base_url", baseUrl + "/");

		cond.execute(env);

		assertEquals(baseUrl + "/userinfo", env.getString("server", "userinfo_endpoint"));
	}

}
