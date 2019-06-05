package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateTokenEndpointRequestForClientCredentialsGrant_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateTokenEndpointRequestForClientCredentialsGrant cond;

	private JsonObject clientWithScope;

	private JsonObject clientWithoutScope;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateTokenEndpointRequestForClientCredentialsGrant();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		clientWithScope = new JsonParser().parse("{\"scope\": \"foo bar\"}").getAsJsonObject();
		clientWithoutScope = new JsonParser().parse("{}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_withScope() {

		env.putObject("client", clientWithScope);

		cond.evaluate(env);

		JsonObject parameters = env.getObject("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(OIDFJSON.getString(parameters.get("grant_type"))).isEqualTo("client_credentials");

		assertThat(parameters.has("scope")).isTrue();
		assertThat(parameters.get("scope")).isEqualTo(clientWithScope.get("scope"));
	}

	@Test
	public void testEvaluate_withoutScope() {

		env.putObject("client", clientWithoutScope);

		cond.evaluate(env);

		JsonObject parameters = env.getObject("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(OIDFJSON.getString(parameters.get("grant_type"))).isEqualTo("client_credentials");

		assertThat(parameters.has("scope")).isFalse();
	}
}
