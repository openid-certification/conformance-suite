package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

	@Test(expected = ConditionError.class)
	public void testEvaluate_isEmpty() {
		JsonObject o = new JsonObject();
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_pragmaMissing() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "no-store, no-transform");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_good() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "no-store, no-transform");
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_isGoodCacheControlArray() {
		// the server can legally sent several Cache-Control headers, which java presents as an array

		JsonArray a = new JsonArray();
		a.add("no-store");
		a.add("no-transform");

		JsonObject o = new JsonObject();
		o.add("cache-control", a);
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badCacheControlArray() {
		// the server can legally sent several Cache-Control headers, which java presents as an array

		JsonArray a = new JsonArray();
		a.add("store");
		a.add("transform");

		JsonObject o = new JsonObject();
		o.add("cache-control", a);
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_cacheControlMissing() {
		JsonObject o = new JsonObject();
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isBadCacheControlEmpty() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "");
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isBadCacheControl() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "store");
		o.addProperty("pragma", "no-cache");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isBadPragma() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "no-store, no-transform");
		o.addProperty("pragma", "");
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}
}
