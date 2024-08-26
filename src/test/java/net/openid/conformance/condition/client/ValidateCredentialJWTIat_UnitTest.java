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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateCredentialJWTIat_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;

	private ValidateCredentialJWTIat cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateCredentialJWTIat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", nowSeconds - 10);
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingIat() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidIatPast() {
		assertThrows(ConditionError.class, () -> {

			JsonObject claims = new JsonObject();
			claims.addProperty("iat", 1514764800);
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_allowableIatSkew() {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", nowSeconds + 3 * 60);
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_futureIat() {
		assertThrows(ConditionError.class, () -> {
			JsonObject claims = new JsonObject();
			claims.addProperty("iat", nowSeconds + 10 * 60);
			env.putObject("sdjwt", "credential.claims", claims);

			cond.execute(env);
		});
	}
}
