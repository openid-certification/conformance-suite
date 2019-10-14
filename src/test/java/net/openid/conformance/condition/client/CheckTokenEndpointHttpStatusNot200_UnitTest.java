package net.openid.conformance.condition.client;

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
		cond = new CheckTokenEndpointHttpStatusNot200();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_HttpStatusCodeNullError() {

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_HttpStatusCode200() {

		env.putInteger("token_endpoint_response_http_status", 200);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_HttpStatusCode400() {

		env.putInteger("token_endpoint_response_http_status", 400);

		cond.execute(env);
	}
}
