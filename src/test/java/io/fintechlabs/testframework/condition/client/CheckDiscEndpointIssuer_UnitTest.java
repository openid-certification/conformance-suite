package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckDiscEndpointIssuer_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckDiscEndpointIssuer cond;

	@Before
	public void setUp() throws Exception {

		cond = new CheckDiscEndpointIssuer();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject config = new JsonObject();
		config.add("server", new JsonParser().parse("{\"discoveryUrl\": \"https://login.microsoftonline.com/5608b4e0-e26d-4bd1-8a1a-d57d7ae2af8c/v2.0/.well-known/openid-configuration\"}"));

		env.putObject("config", config);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseMissingIssuer() {

		JsonObject server = new JsonObject();

		env.putObject("server", server);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseNoMatching() {

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://fapidev-as.authlete.net/");

		env.putObject("server", server);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseInvalidIssuer() {

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "http:/fapidev-as.authlete.net/");

		env.putObject("server", server);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseMatching() {

		JsonObject server = new JsonObject();
		server.addProperty("issuer", "https://login.microsoftonline.com/5608b4e0-e26d-4bd1-8a1a-d57d7ae2af8c/v2.0");

		env.putObject("server", server);

		cond.execute(env);

	}

}
