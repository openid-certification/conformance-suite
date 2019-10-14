package net.openid.conformance.condition.common;

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
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateRandomImplicitSubmitUrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateRandomImplicitSubmitUrl cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CreateRandomImplicitSubmitUrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CreateRandomImplicitSubmitUrl#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putString("base_url", "https://example.com");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("base_url");

		assertThat(env.getObject("implicit_submit")).isNotNull();
		assertThat(env.getString("implicit_submit", "path")).isNotEmpty();
		assertThat(env.getString("implicit_submit", "fullUrl")).isNotEmpty();
	}

	/**
	 * Test method for {@link CreateRandomImplicitSubmitUrl#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.execute(env);
	}
}
