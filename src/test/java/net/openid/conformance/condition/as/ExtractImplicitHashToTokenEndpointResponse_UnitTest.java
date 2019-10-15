package net.openid.conformance.condition.as;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.ExtractImplicitHashToTokenEndpointResponse;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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
		cond = new ExtractImplicitHashToTokenEndpointResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link ExtractImplicitHashToTokenEndpointResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putString("implicit_hash", "#access_token=2YotnFZFEjr1zCsicMWpAA&state=xyz&token_type=example&expires_in=3600");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("implicit_hash");

		assertThat(env.getObject("callback_params")).isNotNull();
		assertThat(env.getObject("callback_params").entrySet().size()).isEqualTo(4);
		assertThat(env.getString("callback_params", "access_token")).isEqualTo("2YotnFZFEjr1zCsicMWpAA");
		assertThat(env.getString("callback_params", "state")).isEqualTo("xyz");
		assertThat(env.getString("callback_params", "token_type")).isEqualTo("example");
		assertThat(env.getString("callback_params", "expires_in")).isEqualTo("3600");
	}

	/**
	 * Test method for {@link ExtractImplicitHashToTokenEndpointResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.execute(env);
	}
}
