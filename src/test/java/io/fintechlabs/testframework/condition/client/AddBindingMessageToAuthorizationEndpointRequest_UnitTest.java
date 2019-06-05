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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddBindingMessageToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddBindingMessageToAuthorizationEndpointRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddBindingMessageToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_presentBindingMessage() {

		JsonObject authorizationEndpointRequest = new JsonObject();

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.evaluate(env);

		assertThat(env.getObject("authorization_endpoint_request").has("binding_message")).isTrue();

		assertThat(env.getString("authorization_endpoint_request", "binding_message")).isEqualTo("1234");

	}

}
