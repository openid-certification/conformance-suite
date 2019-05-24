package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckTokenEndpointHttpStatus503_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointHttpStatus503 cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckTokenEndpointHttpStatus503("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate() {

		env.putInteger("token_endpoint_response_http_status", 503);

		cond.evaluate(env);
	}
}
