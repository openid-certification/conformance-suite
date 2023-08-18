package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
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
	@Before
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

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIat() {
		JsonObject claims = new JsonObject();
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIatPast() {

		JsonObject claims = new JsonObject();
		claims.addProperty("iat", 1514764800);
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_allowableIatSkew() {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", nowSeconds + 3 * 60);
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_futureIat() {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", nowSeconds + 10 * 60);
		env.putObject("sdjwt", "credential.claims", claims);

		cond.execute(env);
	}
}
