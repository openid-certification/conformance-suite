package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckTokenEndpointRetryAfterHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckTokenEndpointRetryAfterHeaders cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckTokenEndpointRetryAfterHeaders();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isEmpty() {

		cond.execute(env);
	}

	@Test
	public void testEvaluate_HasRetryAfter() {
		JsonObject o = new JsonObject();
		o.addProperty(HttpHeaders.RETRY_AFTER, 300);
		env.putObject("token_endpoint_response_headers", o);

		cond.execute(env);
	}

}
