package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AddFormBasedClientSecretAuthenticationParameters_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private JsonObject tokenEndpointRequestFormParameters;

	private AddFormBasedClientSecretAuthenticationParameters cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new AddFormBasedClientSecretAuthenticationParameters();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		client = new JsonParser().parse("{"
			+ "\"client_id\":\"client\","
			+ "\"client_secret\":\"secret\""
			+ "}").getAsJsonObject();

		tokenEndpointRequestFormParameters = new JsonParser().parse("{"
			+ "\"grant_type\":\"unit_test\","
			+ "\"scope\":\"address phone openid email profile\""
			+ "}").getAsJsonObject();

		env.putObject("client", client);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_request_form_parameters", tokenEndpointRequestFormParameters);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client", "client_id");
		verify(env, atLeastOnce()).getString("client", "client_secret");

		assertThat(env.getString("token_endpoint_request_form_parameters", "client_id")).isEqualTo("client");
		assertThat(env.getString("token_endpoint_request_form_parameters", "client_secret")).isEqualTo("secret");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.execute(env);

	}

}
