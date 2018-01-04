package io.fintechlabs.testframework.condition.as;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToTokenEndpointResponse;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractImplicitHashToTokenEndpointResponse_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;
	
	private ExtractImplicitHashToTokenEndpointResponse cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new ExtractImplicitHashToTokenEndpointResponse("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ExtractImplicitHashToTokenEndpointResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {
		
		env.putString("implicit_hash", "#access_token=2YotnFZFEjr1zCsicMWpAA&state=xyz&token_type=example&expires_in=3600");

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("implicit_hash");
		
		assertThat(env.get("callback_params")).isNotNull();
		assertThat(env.get("callback_params").entrySet().size()).isEqualTo(4);
		assertThat(env.getString("callback_params", "access_token")).isEqualTo("2YotnFZFEjr1zCsicMWpAA");
		assertThat(env.getString("callback_params", "state")).isEqualTo("xyz");
		assertThat(env.getString("callback_params", "token_type")).isEqualTo("example");
		assertThat(env.getString("callback_params", "expires_in")).isEqualTo("3600");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ExtractImplicitHashToTokenEndpointResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);
	}
}
