package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ValidateCredentialJWTNbf_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private long nowSeconds;

	private ValidateCredentialJWTNbf cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateCredentialJWTNbf();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
	}

	@Test
	public void testEvaluate_validNbf() {
		JsonObject claims = new JsonObject();
		claims.addProperty("nbf", nowSeconds - 3600); // 1 hour ago
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingNbf() {
		JsonObject claims = new JsonObject();
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);

		verify(eventLog, times(1)).log(anyString(), eq("'nbf' is not present"));
	}

	@Test
	public void testEvaluate_allowableSkew() {
		JsonObject claims = new JsonObject();
		claims.addProperty("nbf", nowSeconds + 3 * 60); // 3 minutes in the future, within 5-min skew
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_futureNbf() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			claims.addProperty("nbf", nowSeconds + 10 * 60); // 10 minutes in the future
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_unreasonablyOld() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			claims.addProperty("nbf", 0L); // epoch
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_millisecondConfusion() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			claims.addProperty("nbf", nowSeconds * 1000); // milliseconds instead of seconds
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);
		});
	}
}
