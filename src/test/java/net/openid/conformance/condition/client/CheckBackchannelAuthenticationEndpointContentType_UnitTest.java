package net.openid.conformance.condition.client;

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
public class CheckBackchannelAuthenticationEndpointContentType_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckBackchannelAuthenticationEndpointContentType cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckBackchannelAuthenticationEndpointContentType();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void setHeader(Environment env, String value) {

		JsonObject headers = new JsonObject();
		headers.addProperty("content-type", value);
		env.putObject("backchannel_authentication_endpoint_response_headers", headers);

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
