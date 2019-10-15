package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

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

		cond.execute(env);

		String expectedRequestedExpiry = "30";
		assertEquals(env.getString("authorization_endpoint_request", "requested_expiry"), expectedRequestedExpiry);
	}
}
