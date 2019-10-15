package net.openid.conformance.condition.client;

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
		cond = new CreateRedirectUri();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putString("base_url", "https://example.com");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("base_url");

		assertThat(env.getString("redirect_uri")).isEqualTo("https://example.com/callback");

	}

	@Test
	public void testEvaluate_valueAndSuffixPresent() {

		env.putString("base_url", "https://example.com");

		env.putString("redirect_uri_suffix", "?dummy1=lorem&dummy2=ipsum");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("base_url");
		verify(env, atLeastOnce()).getString("redirect_uri_suffix");

		assertThat(env.getString("redirect_uri")).isEqualTo("https://example.com/callback?dummy1=lorem&dummy2=ipsum");

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueEmpty() {

		env.putString("base_url", "");

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueNull() {

		env.putString("base_url", null);

		cond.execute(env);

	}
}
