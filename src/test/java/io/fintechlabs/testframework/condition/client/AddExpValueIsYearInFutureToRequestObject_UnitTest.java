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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddExpValueIsYearInFutureToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddExpValueIsYearInFutureToRequestObject cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddExpValueIsYearInFutureToRequestObject("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_presentExpValue() {

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		assertThat(env.getObject("request_object_claims").has("exp")).isTrue();

	}

	@Test
	public void testEvaluate_expValueIsOneYearInTheFuture() {

		long expExpect = 60 * 60 * 24 * 365;

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		assertThat(env.getLong("request_object_claims", "exp") - Instant.now().getEpochSecond()).isEqualTo(expExpect);

	}

}
