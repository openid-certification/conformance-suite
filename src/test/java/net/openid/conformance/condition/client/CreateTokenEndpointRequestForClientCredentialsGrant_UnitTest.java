package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CreateTokenEndpointRequestForClientCredentialsGrant();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		clientWithScope = JsonParser.parseString("{\"scope\": \"foo bar\"}").getAsJsonObject();
		clientWithoutScope = JsonParser.parseString("{}").getAsJsonObject();

	}

	/**
	 * Test method for {@link CreateTokenEndpointRequestForClientCredentialsGrant#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_withScope() {

		env.putObject("client", clientWithScope);

		cond.execute(env);

		JsonObject parameters = env.getObject("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(OIDFJSON.getString(parameters.get("grant_type"))).isEqualTo("client_credentials");

		assertThat(parameters.has("scope")).isTrue();
		assertThat(parameters.get("scope")).isEqualTo(clientWithScope.get("scope"));
	}

	@Test
	public void testEvaluate_withoutScope() {

		env.putObject("client", clientWithoutScope);

		cond.execute(env);

		JsonObject parameters = env.getObject("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(OIDFJSON.getString(parameters.get("grant_type"))).isEqualTo("client_credentials");

		assertThat(parameters.has("scope")).isFalse();
	}
}
