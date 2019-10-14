package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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

		cond = new SetAccountScopeOnTokenEndpointRequest();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("token_endpoint_request_form_parameters", tokenEndpointRequest);

	}

	/**
	 * Test method for {@link CreateTokenEndpointRequestForClientCredentialsGrant#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate() {

		cond.execute(env);

		JsonObject parameters = env.getObject("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(OIDFJSON.getString(parameters.get("scope"))).isEqualTo("accounts");

	}

}
