package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

@RunWith(MockitoJUnitRunner.class)
public class VerifyIdTokenSubConsistentHybridFlow_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodToken;

	private VerifyIdTokenSubConsistentHybridFlow cond;

	@Before
	public void setUp() throws Exception {

		cond = new VerifyIdTokenSubConsistentHybridFlow();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Good sample from OpenID Connect Core spec

		JsonObject goodClaims = new JsonParser().parse("{\n" +
			" \"sub\": \"248289761001\"\n" +
			"}").getAsJsonObject();

		goodToken = new JsonObject();
		goodToken.add("claims", goodClaims);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("authorization_endpoint_id_token", goodToken);
		env.putObject("token_endpoint_id_token", goodToken);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMismatch() {

		JsonObject badToken = goodToken.deepCopy();
		badToken.get("claims").getAsJsonObject().addProperty("sub", "foo");
		env.putObject("authorization_endpoint_id_token", goodToken);
		env.putObject("token_endpoint_id_token", badToken);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		JsonObject badToken = goodToken.deepCopy();
		badToken.get("claims").getAsJsonObject().remove("sub");
		env.putObject("authorization_endpoint_id_token", goodToken);
		env.putObject("token_endpoint_id_token", badToken);

		cond.execute(env);
	}

	@Test(expected = NullPointerException.class)
	public void testEvaluate_missingAuth() {

		JsonObject badToken = goodToken.deepCopy();
		badToken.get("claims").getAsJsonObject().remove("sub");
		env.putObject("authorization_endpoint_id_token", badToken);
		env.putObject("token_endpoint_id_token", goodToken);

		cond.execute(env);
	}

}
