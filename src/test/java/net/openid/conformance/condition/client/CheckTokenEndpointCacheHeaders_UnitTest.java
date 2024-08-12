package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckTokenEndpointCacheHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointCacheHeaders cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckTokenEndpointCacheHeaders();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			env.putObject("token_endpoint_response_headers", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_good() {
		JsonObject o = new JsonObject();
		o.addProperty("cache-control", "no-store, no-transform");
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
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_badCacheControlArray() {
		assertThrows(ConditionError.class, () -> {
			// the server can legally sent several Cache-Control headers, which java presents as an array

			JsonArray a = new JsonArray();
			a.add("store");
			a.add("transform");

			JsonObject o = new JsonObject();
			o.add("cache-control", a);
			env.putObject("token_endpoint_response_headers", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_cacheControlMissing() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			env.putObject("token_endpoint_response_headers", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_isBadCacheControlEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("cache-control", "");
			env.putObject("token_endpoint_response_headers", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_isBadCacheControl() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.addProperty("cache-control", "store");
			env.putObject("token_endpoint_response_headers", o);

			cond.execute(env);
		});
	}
}
