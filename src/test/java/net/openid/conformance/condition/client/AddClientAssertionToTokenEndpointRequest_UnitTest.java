package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddClientAssertionToTokenEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddClientAssertionToTokenEndpointRequest cond;

	private String clientAssertion;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddClientAssertionToTokenEndpointRequest();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		clientAssertion = "client.assertion.string"; // note that this is normally a JWT calculated by another module, this module just copies the value
	}

	/**
	 * Test method for {@link AddClientAssertionToTokenEndpointRequest#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate() {

		env.putObject("token_endpoint_request_form_parameters", new JsonObject());
		env.putString("client_assertion", clientAssertion);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_assertion");

		assertThat(env.getString("token_endpoint_request_form_parameters", "client_assertion")).isEqualTo(clientAssertion);
		assertThat(env.getString("token_endpoint_request_form_parameters", "client_assertion_type")).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

	}

	/**
	 * Test method for {@link AddClientAssertionToTokenEndpointRequest#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingForm() {
		assertThrows(ConditionError.class, () -> {

			env.putString("client_assertion", clientAssertion);

			cond.execute(env);

		});

	}

}
