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
public class AddAcrValuesScaToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddAcrValuesScaToAuthorizationEndpointRequest cond;

	private String acrValue = "urn:openbanking:psd2:sca";

	@Before
	public void setUp() throws Exception {
		cond = new AddAcrValuesScaToAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		env.putObject("authorization_endpoint_request", new JsonObject());

		cond.evaluate(env);

		assertThat(env.getString("authorization_endpoint_request", "acr_values")).isNotEmpty();
		assertThat(env.getString("authorization_endpoint_request", "acr_values")).isEqualTo(acrValue);
	}
}
