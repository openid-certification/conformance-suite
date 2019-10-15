package net.openid.conformance.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureResourceResponseContentTypeIsJsonUTF8_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureResourceResponseContentTypeIsJsonUTF8 cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureResourceResponseContentTypeIsJsonUTF8();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json; charset=UTF-8");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "content-type");
	}

	/**
	 * Test method for {@link EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidCharset() {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json; charset=Shift_JIS");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingCharset() {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "application/json");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidType() {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", "text/json; charset=UTF-8");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

	/**
	 * Test method for {@link EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingContentType() {

		JsonObject headers = new JsonObject();
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

}
