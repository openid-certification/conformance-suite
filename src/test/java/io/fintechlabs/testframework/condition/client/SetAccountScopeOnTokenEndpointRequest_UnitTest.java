package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class SetAccountScopeOnTokenEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetAccountScopeOnTokenEndpointRequest cond;

	private JsonObject tokenEndpointRequest = new JsonObject();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new SetAccountScopeOnTokenEndpointRequest("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("token_endpoint_request_form_parameters", tokenEndpointRequest);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate() {

		cond.evaluate(env);

		JsonObject parameters = env.getObject("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(parameters.get("scope").getAsString()).isEqualTo("accounts");

	}

}
