package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckBackchannelAuthenticationEndpointHttpStatus400_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckBackchannelAuthenticationEndpointHttpStatus400 cond;

	@Before
	public void setUp() throws Exception {

		cond = new CheckBackchannelAuthenticationEndpointHttpStatus400("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_noError() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 400);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_withError400() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 200);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongParameters() {
		env.putInteger("authentication_endpoint_response_http_status", 400);

		cond.evaluate(env);
	}

}
