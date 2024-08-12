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
public class VerifyIdTokenSubConsistentHybridFlow_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodToken;

	private VerifyIdTokenSubConsistentHybridFlow cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new VerifyIdTokenSubConsistentHybridFlow();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Good sample from OpenID Connect Core spec

		JsonObject goodClaims = JsonParser.parseString("""
				{
				 "sub": "248289761001"
				}""").getAsJsonObject();

		goodToken = new JsonObject();
		goodToken.add("claims", goodClaims);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("authorization_endpoint_id_token", goodToken);
		env.putObject("token_endpoint_id_token", goodToken);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_valueMismatch() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badToken = goodToken.deepCopy();
			badToken.get("claims").getAsJsonObject().addProperty("sub", "foo");
			env.putObject("authorization_endpoint_id_token", goodToken);
			env.putObject("token_endpoint_id_token", badToken);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingToken() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badToken = goodToken.deepCopy();
			badToken.get("claims").getAsJsonObject().remove("sub");
			env.putObject("authorization_endpoint_id_token", goodToken);
			env.putObject("token_endpoint_id_token", badToken);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingAuth() {
		assertThrows(NullPointerException.class, () -> {

			JsonObject badToken = goodToken.deepCopy();
			badToken.get("claims").getAsJsonObject().remove("sub");
			env.putObject("authorization_endpoint_id_token", badToken);
			env.putObject("token_endpoint_id_token", goodToken);

			cond.execute(env);
		});
	}

}
