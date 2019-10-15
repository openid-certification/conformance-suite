package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
public class VerifyBearerTokenHeaderCallback_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyBearerTokenHeaderCallback cond;

	@Before
	public void setUp() throws Exception {

		cond = new VerifyBearerTokenHeaderCallback();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		env.putString("client_notification_token", "8d67dc78-7faa-4d41-aabd-67707b374255");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseHeadersAsNull() {
		env.putObject("notification_callback", new JsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseHeadersAsEmpty() {
		JsonObject o = new JsonObject();
		o.add("headers", new JsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseAuthorizationAsEmpty() {
		JsonObject o = new JsonObject();
		o.add("headers", new JsonParser().parse("{\"authorization\": \"\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseAuthorizationAsNotBearer() {
		JsonObject o = new JsonObject();
		o.add("headers", new JsonParser().parse("{\"authorization\": \"x-bearer 8d67dc78-7faa-4d41-aabd-67707b374255\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject o = new JsonObject();
		o.add("headers", new JsonParser().parse("{\"authorization\": \"Bearer 8d67dc78-7faa-4d41-aabd-67707b374255\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseGoodMultiSpaces() {
		JsonObject o = new JsonObject();
		o.add("headers", new JsonParser().parse("{\"authorization\": \"Bearer   8d67dc78-7faa-4d41-aabd-67707b374255\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBad() {
		JsonObject o = new JsonObject();
		o.add("headers", new JsonParser().parse("{\"authorization\": \"Bearer qw3lPzKZNTpgZ2IKYSNwn6Xct1pX9jdQ2dIBUpD4AiI\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}
}
