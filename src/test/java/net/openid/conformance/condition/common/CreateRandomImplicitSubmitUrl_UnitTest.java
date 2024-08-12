package net.openid.conformance.condition.common;

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
public class CreateRandomImplicitSubmitUrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateRandomImplicitSubmitUrl cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
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
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);
		});
	}
}
