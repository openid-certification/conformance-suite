package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureClientIdMatchesResponseUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureClientIdMatchesResponseUri cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureClientIdMatchesResponseUri();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_matchingWithPrefix() {
		JsonObject effective = new JsonObject();
		effective.addProperty("client_id", "redirect_uri:https://example.com/callback");
		effective.addProperty("response_uri", "https://example.com/callback");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effective);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_matchingWithoutPrefix() {
		JsonObject effective = new JsonObject();
		effective.addProperty("client_id", "https://example.com/callback");
		effective.addProperty("response_uri", "https://example.com/callback");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effective);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_mismatch() {
		JsonObject effective = new JsonObject();
		effective.addProperty("client_id", "redirect_uri:https://example.com/other");
		effective.addProperty("response_uri", "https://example.com/callback");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effective);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingClientId() {
		JsonObject effective = new JsonObject();
		effective.addProperty("response_uri", "https://example.com/callback");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effective);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingResponseUri() {
		// OID4VP 1.0 Final section 5.9.3: Verifier MAY omit response_uri
		JsonObject effective = new JsonObject();
		effective.addProperty("client_id", "https://example.com/callback");
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effective);

		cond.execute(env);
	}
}
