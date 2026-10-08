package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidateIdTokenNonce_UnitTest {

	private Environment env;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateIdTokenNonce cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateIdTokenNonce();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env = new Environment();
	}

	private void putIdTokenWithNonce(String nonce) {
		JsonObject claims = new JsonObject();
		claims.addProperty("sub", "248289761001");
		if (nonce != null) {
			claims.addProperty("nonce", nonce);
		}
		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.putObject("id_token", idToken);
	}

	@Test
	public void testEvaluate_noncesMatch() {
		env.putString("nonce", "n-0S6_WzA2Mj");
		putIdTokenWithNonce("n-0S6_WzA2Mj");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noncesMismatch() {
		env.putString("nonce", "n-0S6_WzA2Mj");
		putIdTokenWithNonce("something-else");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_nonceSentButMissingFromIdToken() {
		env.putString("nonce", "n-0S6_WzA2Mj");
		putIdTokenWithNonce(null);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noNonceSentAndNoneInIdToken() {
		putIdTokenWithNonce(null);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_noNonceSentButIdTokenContainsNonce() {
		putIdTokenWithNonce("n-0S6_WzA2Mj");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
