package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
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

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AddRequestedExpToAuthorizationEndpointRequestResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddRequestedExpToAuthorizationEndpointRequestResponse cond;

	private Integer requestedExpiry = 30;

	@Before
	public void setUp() throws Exception {

		cond = new AddRequestedExpToAuthorizationEndpointRequestResponse("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_NoRequestedExpiryFieldInClient() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.evaluate(env);

		assertEquals(env.getInteger("authorization_endpoint_request", "requested_expiry"), requestedExpiry);
	}

	@Test
	public void testEvaluate_RequestedExpiryFieldValid() {
		JsonObject client = new JsonObject();
		client.addProperty("requested_expiry", 30);
		env.putObject("client", client);

		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.evaluate(env);

		assertEquals(env.getInteger("authorization_endpoint_request", "requested_expiry"), requestedExpiry);
	}
}
