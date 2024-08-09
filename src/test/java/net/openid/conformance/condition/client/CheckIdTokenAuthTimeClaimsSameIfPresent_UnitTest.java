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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckIdTokenAuthTimeClaimsSameIfPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject claims;

	private CheckIdTokenAuthTimeClaimsSameIfPresent cond;

	private JsonObject createToken(String json) {
		var claims = JsonParser.parseString(json).getAsJsonObject();

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);

		return idToken;
	}

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckIdTokenAuthTimeClaimsSameIfPresent();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_differentAuthTimes() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
			env.putObject("id_token", createToken("{ \"auth_time\": 16 }"));

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noAuthTimes() {
		env.putObject("first_id_token", createToken("{ }"));
		env.putObject("id_token", createToken("{ }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_onlyFirstAuthTime() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
		env.putObject("id_token", createToken("{ }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_onlySecondAuthTime() {
		env.putObject("first_id_token", createToken("{ }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 16 }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_sameAuthTimes() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 15 }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_nullAuthTimes() {
		assertThrows(RuntimeException.class, () -> {
			env.putObject("first_id_token", createToken("{ \"auth_time\": null }"));
			env.putObject("id_token", createToken("{ \"auth_time\": 15 }"));

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_authTimeStrings() {
		assertThrows(RuntimeException.class, () -> {
			env.putObject("first_id_token", createToken("{ \"auth_time\": \"15\" }"));
			env.putObject("id_token", createToken("{ \"auth_time\": \"16\" }"));

			cond.execute(env);
		});
	}

}
