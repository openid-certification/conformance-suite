package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
public class VerifyBearerTokenHeaderCallback_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyBearerTokenHeaderCallback cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new VerifyBearerTokenHeaderCallback();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		env.putString("client_notification_token", "8d67dc78-7faa-4d41-aabd-67707b374255");
	}

	@Test
	public void testEvaluate_caseHeadersAsNull() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("notification_callback", new JsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseHeadersAsEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.add("headers", new JsonObject());
			env.putObject("notification_callback", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseAuthorizationAsEmpty() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.add("headers", JsonParser.parseString("{\"authorization\": \"\"}").getAsJsonObject());
			env.putObject("notification_callback", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseAuthorizationAsNotBearer() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.add("headers", JsonParser.parseString("{\"authorization\": \"x-bearer 8d67dc78-7faa-4d41-aabd-67707b374255\"}").getAsJsonObject());
			env.putObject("notification_callback", o);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject o = new JsonObject();
		o.add("headers", JsonParser.parseString("{\"authorization\": \"Bearer 8d67dc78-7faa-4d41-aabd-67707b374255\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseGoodMultiSpaces() {
		JsonObject o = new JsonObject();
		o.add("headers", JsonParser.parseString("{\"authorization\": \"Bearer   8d67dc78-7faa-4d41-aabd-67707b374255\"}").getAsJsonObject());
		env.putObject("notification_callback", o);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseBad() {
		assertThrows(ConditionError.class, () -> {
			JsonObject o = new JsonObject();
			o.add("headers", JsonParser.parseString("{\"authorization\": \"Bearer qw3lPzKZNTpgZ2IKYSNwn6Xct1pX9jdQ2dIBUpD4AiI\"}").getAsJsonObject());
			env.putObject("notification_callback", o);

			cond.execute(env);
		});
	}
}
