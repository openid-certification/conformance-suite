package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
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
public class CheckTokenEndpointHttpStatusNot200_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointHttpStatusNot200 cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckTokenEndpointHttpStatusNot200("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_HttpStatusCodeNullError() {

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_HttpStatusCode200() {

		env.putInteger("token_endpoint_response_http_status", 200);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_HttpStatusCode400() {

		env.putInteger("token_endpoint_response_http_status", 400);

		cond.evaluate(env);
	}
}
