package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AddRequestedExp30SToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddRequestedExp30sToAuthorizationEndpointRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddRequestedExp30sToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_RequestedExpiryFieldValid() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.execute(env);

		Integer expectedRequestedExpiry = 30;
		assertEquals(env.getInteger("authorization_endpoint_request", "requested_expiry"), expectedRequestedExpiry);
	}
}
