package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AddClientAssertionToBackchannelAuthenticationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddClientAssertionToBackchannelAuthenticationEndpoint cond;

	private String clientAssertion;

	@Before
	public void setUp() throws Exception {

		cond = new AddClientAssertionToBackchannelAuthenticationEndpoint();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		clientAssertion = "client.assertion.string"; // note that this is normally a JWT calculated by another module, this module just copies the value
	}

	@Test
	public void testEvaluate() {

		env.putObject("backchannel_authentication_endpoint_request_form_parameters", new JsonObject());
		env.putString("client_assertion", clientAssertion);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("client_assertion");

		assertThat(env.getString("backchannel_authentication_endpoint_request_form_parameters", "client_assertion")).isEqualTo(clientAssertion);
		assertThat(env.getString("backchannel_authentication_endpoint_request_form_parameters", "client_assertion_type")).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

	}

}
