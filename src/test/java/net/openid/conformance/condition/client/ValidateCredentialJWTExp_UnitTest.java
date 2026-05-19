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
public class ValidateCredentialJWTExp_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private long nowSeconds;

	private ValidateCredentialJWTExp cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateCredentialJWTExp();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
	}

	@Test
	public void testEvaluate_validExp() {
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", nowSeconds + 3600); // 1 hour from now
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingExp() {
		JsonObject claims = new JsonObject();
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);

		verify(eventLog, times(1)).log(anyString(), eq("'exp' is not present"));
	}

	@Test
	public void testEvaluate_recentlyExpiredWithinSkew() {
		// Expired 10 seconds ago, but within 5-minute clock skew tolerance
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", nowSeconds - 10);
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_expiredBeyondSkew() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			claims.addProperty("exp", nowSeconds - 600); // 10 minutes ago, beyond 5-minute skew
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_unreasonablyFarInFuture() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			// 51 years in the future in seconds, likely a milliseconds mistake
			claims.addProperty("exp", nowSeconds * 1000);
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);
		});
	}
}
