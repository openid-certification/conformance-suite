package net.openid.conformance.condition.as;

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
public class OIDCCValidateRequestObjectExp_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCValidateRequestObjectExp cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new OIDCCValidateRequestObjectExp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addRequestObject(long exp) {
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", exp);
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}

	@Test
	public void testExpReasonable() {
		addRequestObject(Instant.now().getEpochSecond() + 300);
		cond.execute(env);
	}

	@Test
	public void testExpMillisAsSeconds() {
		assertThrows(ConditionError.class, () -> {
			addRequestObject(Instant.now().getEpochSecond() * 1000);
			cond.execute(env);
		});
	}

	@Test
	public void testExpExpired() {
		assertThrows(ConditionError.class, () -> {
			addRequestObject(Instant.now().getEpochSecond() - 3600);
			cond.execute(env);
		});
	}
}
