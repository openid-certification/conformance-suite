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
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AddIatValueIsWeekInPastToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddIatValueIsWeekInPastToRequestObject cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddIatValueIsWeekInPastToRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_presentIatValue() {

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		assertThat(env.getObject("request_object_claims").has("iat")).isTrue();

	}

	@Test
	public void testEvaluate_iatValueIsOneWeekInThePast() {

		long iatExpect = 60 * 60 * 24 * 7;

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.evaluate(env);

		assertThat(Instant.now().getEpochSecond() - env.getLong("request_object_claims", "iat")).isEqualTo(iatExpect);

	}

}
