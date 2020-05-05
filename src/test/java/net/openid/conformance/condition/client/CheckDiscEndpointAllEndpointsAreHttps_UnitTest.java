package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckDiscEndpointAllEndpointsAreHttps_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckDiscEndpointAllEndpointsAreHttps cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckDiscEndpointAllEndpointsAreHttps();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"flibble_endpoint\": \"https://www.example.com/endpoint\""
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorOtherTypes () {
		JsonObject server = new JsonParser().parse("{"
			+ "\"flibble_endpoint\": \"https://www.example.com/endpoint\","
			+ "\"flibble\" : true,"
			+ "\"flibble2\" : 0.9,"
			+ "\"flibble3\" : { \"a\": \"b\"},"
			+ "\"flibble4\" : [ \"a\"]"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notHttps() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"flibble_endpoint\": \"http://www.example.com/endpoint\""
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notUrl() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"flibble_endpoint\": \"flibble\""
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorArray() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"flibble_endpoint\": ["
			+ "\"https://www.example.com/endpoint\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

}
