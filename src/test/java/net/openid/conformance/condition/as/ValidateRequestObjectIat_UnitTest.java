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
public class ValidateRequestObjectIat_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateRequestObjectIat cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateRequestObjectIat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addRequestObject(long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", iat);
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}

	@Test
	public void testIatRecent() {
		addRequestObject(Instant.now().getEpochSecond() - 10);
		cond.execute(env);
	}

	@Test
	public void testIatTooOld() {
		assertThrows(ConditionError.class, () -> {
			addRequestObject(Instant.now().getEpochSecond() - 2 * 86400);
			cond.execute(env);
		});
	}

	@Test
	public void testIatInFuture() {
		assertThrows(ConditionError.class, () -> {
			addRequestObject(Instant.now().getEpochSecond() + 3600);
			cond.execute(env);
		});
	}
}
