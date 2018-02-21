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

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateRedirectUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateRedirectUri cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateRedirectUri("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CreateRedirectUri#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putString("base_url", "https://example.com");

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("base_url");

		assertThat(env.getString("redirect_uri")).isEqualTo("https://example.com/callback");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CreateRedirectUri#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate_valueAndSuffixPresent() {

		env.putString("base_url", "https://example.com");

		env.putString("redirect_uri_suffix", "?dummy1=lorem&dummy2=ipsum");

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("base_url");
		verify(env, atLeastOnce()).getString("redirect_uri_suffix");

		assertThat(env.getString("redirect_uri")).isEqualTo("https://example.com/callback?dummy1=lorem&dummy2=ipsum");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CreateRedirectUri#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueEmpty() {

		env.putString("base_url", "");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CreateRedirectUri#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueNull() {

		env.putString("base_url", null);

		cond.evaluate(env);

	}
}
