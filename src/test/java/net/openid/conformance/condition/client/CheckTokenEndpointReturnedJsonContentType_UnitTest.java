package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class CheckTokenEndpointReturnedJsonContentType_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointReturnedJsonContentType cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CheckTokenEndpointReturnedJsonContentType();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setHeader(Environment env, String value) {

		JsonObject headers = new JsonObject();
		if (value != null) {
			headers.addProperty("content-type", value);
		}
		env.putObject("token_endpoint_response_headers", headers);

	}

	@Test
	public void testEvaluate_noError() {
		setHeader(env, "application/json");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_withCharset() {
		setHeader(env, "application/json   ; charset=UTF-8");

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalid() {
		setHeader(env, "application/jsonmoo");

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notFoundContentType() {
		setHeader(env, null);
		cond.execute(env);
	}

}
