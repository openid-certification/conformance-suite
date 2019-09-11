package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RunWith(MockitoJUnitRunner.class)
public class AddExpIs5MinutesInPastToClientAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddExpIs5MinutesInPastToClientAssertionClaims cond;

	@Before
	public void setUp() throws Exception {
		cond = new AddExpIs5MinutesInPastToClientAssertionClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {

		JsonObject clientAssertClaims = new JsonObject();

		clientAssertClaims.addProperty("jti", RandomStringUtils.randomAlphanumeric(20));

		env.putObject("client_assertion_claims", clientAssertClaims);

		cond.evaluate(env);

		assertThat(env.getObject("client_assertion_claims")).isNotNull();

		JsonObject claims = env.getObject("client_assertion_claims");

		assertThat(OIDFJSON.getLong(claims.get("exp"))).isCloseTo(Instant.now().minusSeconds(5*60).getEpochSecond(), within(5L)); // 300 seconds in the past, 5 second leeway

	}
}
