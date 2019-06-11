package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
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
public class CheckTokenEndpointCacheHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointCacheHeaders cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckTokenEndpointCacheHeaders();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isEmpty() {
		JsonObject o = new JsonObject();
		env.putObject("token_endpoint_response_headers", o);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_isGoodCacheControl() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "no-store, no-transform");
		env.putObject("token_endpoint_response_headers", o);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_isGoodPragma() {
		JsonObject o = new JsonObject();
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isBadCacheControl() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "");
		env.putObject("token_endpoint_response_headers", o);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isBadPragma() {
		JsonObject o = new JsonObject();
		o.addProperty("pragma", "");
		env.putObject("token_endpoint_response_headers", o);

		cond.evaluate(env);
	}
}
