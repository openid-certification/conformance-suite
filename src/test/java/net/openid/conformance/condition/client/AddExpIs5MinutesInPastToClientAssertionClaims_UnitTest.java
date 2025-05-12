package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
public class AddExpIs5MinutesInPastToClientAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddExpIs5MinutesInPastToClientAssertionClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddExpIs5MinutesInPastToClientAssertionClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {

		JsonObject clientAssertClaims = new JsonObject();

		clientAssertClaims.addProperty("jti", RandomStringUtils.secure().nextAlphanumeric(20));

		env.putObject("client_assertion_claims", clientAssertClaims);

		cond.execute(env);

		assertThat(env.getObject("client_assertion_claims")).isNotNull();

		JsonObject claims = env.getObject("client_assertion_claims");

		assertThat(OIDFJSON.getLong(claims.get("exp"))).isCloseTo(Instant.now().minusSeconds(5*60).getEpochSecond(), within(5L)); // 300 seconds in the past, 5 second leeway

	}
}
