package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
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
public class CreateInvalidCIBANotificationEndpointUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateInvalidCIBANotificationEndpointUri cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateInvalidCIBANotificationEndpointUri();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putString("base_url", "https://example.com");

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("base_url");

		assertThat(env.getString("invalid_notification_uri")).isEqualTo("https://example.com/invalid-ciba-notification-endpoint");

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueEmpty() {

		env.putString("base_url", "");

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueNull() {

		env.putString("base_url", null);

		cond.evaluate(env);

	}
}
