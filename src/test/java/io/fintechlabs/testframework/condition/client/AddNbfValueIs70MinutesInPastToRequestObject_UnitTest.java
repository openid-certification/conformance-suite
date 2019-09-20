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
public class AddNbfValueIs70MinutesInPastToRequestObject_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddNbfValueIs70MinutesInPastToRequestObject cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddNbfValueIs70MinutesInPastToRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_presentNbfValue() {

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("nbf")).isTrue();

	}

	@Test
	public void testEvaluate_nbfValueIs70MinutesInThePast() {

		long nbfExpect = 70 * 60;

		JsonObject requestObjectClaims = new JsonObject();

		env.putObject("request_object_claims", requestObjectClaims);

		cond.execute(env);

		assertThat(Instant.now().getEpochSecond() - env.getLong("request_object_claims", "nbf")).isEqualTo(nbfExpect);

	}

}
