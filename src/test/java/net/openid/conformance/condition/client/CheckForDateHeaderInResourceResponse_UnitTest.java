package net.openid.conformance.condition.client;

import java.util.Date;

import org.apache.http.client.utils.DateUtils;
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
public class CheckForDateHeaderInResourceResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForDateHeaderInResourceResponse cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckForDateHeaderInResourceResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckForDateHeaderInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("date", DateUtils.formatDate(new Date()));
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "date");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_oldDate() {

		// Obviously, don't run this test on 6 Nov 1994 ;)

		JsonObject headers = new JsonObject();
		headers.addProperty("date", "Sun, 06 Nov 1994 08:49:37 GMT"); // Example from RFC 7231
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "date");
	}

	/**
	 * Test method for {@link CheckForDateHeaderInResourceResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidDate() {

		JsonObject headers = new JsonObject();
		headers.addProperty("date", "this is not a date");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckForDateHeaderInResourceResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingDate() {

		JsonObject headers = new JsonObject();
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);
	}

}
