package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AddRequestedExp30SToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddRequestedExp30sToAuthorizationEndpointRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddRequestedExp30sToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_RequestedExpiryFieldValid() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.evaluate(env);

		Integer expectedRequestedExpiry = 30;
		assertEquals(env.getInteger("authorization_endpoint_request", "requested_expiry"), expectedRequestedExpiry);
	}
}
