package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddClientAssertionToBackchannelAuthenticationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddClientAssertionToBackchannelAuthenticationEndpoint cond;

	private String clientAssertion;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddClientAssertionToBackchannelAuthenticationEndpoint();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		clientAssertion = "client.assertion.string"; // note that this is normally a JWT calculated by another module, this module just copies the value
	}

	@Test
	public void testEvaluate() {

		env.putObject("backchannel_authentication_endpoint_request_form_parameters", new JsonObject());
		env.putString("client_assertion", clientAssertion);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_assertion");

		assertThat(env.getString("backchannel_authentication_endpoint_request_form_parameters", "client_assertion")).isEqualTo(clientAssertion);
		assertThat(env.getString("backchannel_authentication_endpoint_request_form_parameters", "client_assertion_type")).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

	}

}
