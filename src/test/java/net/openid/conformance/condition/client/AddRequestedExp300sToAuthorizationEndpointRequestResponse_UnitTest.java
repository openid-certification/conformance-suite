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
public class AddRequestedExp300sToAuthorizationEndpointRequestResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddRequestedExp300SToAuthorizationEndpointRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddRequestedExp300SToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_RequestedExpiryFieldValid() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.execute(env);

		String expectedRequestedExpiry = "300";
		assertEquals(env.getString("authorization_endpoint_request", "requested_expiry"), expectedRequestedExpiry);
	}
}
