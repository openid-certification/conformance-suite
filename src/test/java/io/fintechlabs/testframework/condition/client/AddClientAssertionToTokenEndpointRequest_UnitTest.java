package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
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
	@Before
	public void setUp() throws Exception {
		
		cond = new AddClientAssertionToTokenEndpointRequest("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		clientAssertion = "client.assertion.string"; // note that this is normally a JWT calculated by another module, this module just copies the value
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CreateRedirectUri#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate() {
		
		env.put("token_endpoint_request_form_parameters", new JsonObject());
		env.putString("client_assertion", clientAssertion);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("client_assertion");
		
		assertThat(env.getString("token_endpoint_request_form_parameters", "client_assertion")).isEqualTo(clientAssertion);
		assertThat(env.getString("token_endpoint_request_form_parameters", "client_assertion_type")).isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CreateRedirectUri#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingForm() {
		
		env.putString("client_assertion", clientAssertion);

		cond.evaluate(env);
		
	}
	
}
