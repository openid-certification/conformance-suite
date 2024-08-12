package net.openid.conformance.condition.client;

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
public class CreateRedirectUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateRedirectUri cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
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

	@Test
	public void testEvaluate_valueEmpty() {
		assertThrows(ConditionError.class, () -> {

			env.putString("base_url", "");

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_valueNull() {
		assertThrows(ConditionError.class, () -> {

			env.putString("base_url", null);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);

		});

	}
}
