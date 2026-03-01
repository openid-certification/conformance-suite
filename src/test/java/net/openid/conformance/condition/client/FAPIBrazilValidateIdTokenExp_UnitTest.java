package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilValidateIdTokenExp_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilValidateIdTokenExp cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIBrazilValidateIdTokenExp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addIdToken(long exp) {
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", exp);
		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.putObject("id_token", idToken);
	}

	@Test
	public void testExpReasonable() {
		// 200 days from now satisfies the 180-day minimum
		addIdToken(Instant.now().getEpochSecond() + 200 * 86400);
		cond.execute(env);
	}

	@Test
	public void testExpMillisAsSeconds() {
		assertThrows(ConditionError.class, () -> {
			addIdToken(Instant.now().getEpochSecond() * 1000);
			cond.execute(env);
		});
	}

	@Test
	public void testExpTooShort() {
		assertThrows(ConditionError.class, () -> {
			addIdToken(Instant.now().getEpochSecond() + 3600);
			cond.execute(env);
		});
	}
}
