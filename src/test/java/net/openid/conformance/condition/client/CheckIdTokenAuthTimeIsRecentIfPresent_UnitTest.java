package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class CheckIdTokenAuthTimeIsRecentIfPresent_UnitTest {
	private long nowSeconds;

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject claims;

	private CheckIdTokenAuthTimeIsRecentIfPresent cond;

	private JsonObject createToken(String json) {
		var claims = JsonParser.parseString(json).getAsJsonObject();

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);

		return idToken;
	}

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckIdTokenAuthTimeIsRecentIfPresent();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
	}

	@Test
	public void testEvaluate_good() {
		env.putObject("id_token", createToken("{ \"auth_time\": "+nowSeconds+" }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_tooOld() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("id_token", createToken("{ \"auth_time\": " + (nowSeconds - 10 * 60) + " }"));

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noAuthTime() {
		env.putObject("id_token", createToken("{ }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nullAuthTimes() {
		assertThrows(RuntimeException.class, () -> {
			env.putObject("id_token", createToken("{ \"auth_time\": null }"));

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_authTimeString() {
		assertThrows(RuntimeException.class, () -> {
			env.putObject("id_token", createToken("{ \"auth_time\": \"16\" }"));

			cond.execute(env);
		});
	}

}
