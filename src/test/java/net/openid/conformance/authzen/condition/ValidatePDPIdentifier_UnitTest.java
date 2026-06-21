package net.openid.conformance.authzen.condition;

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
class ValidatePDPIdentifier_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPIdentifier cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPIdentifier();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putIdentifier(String value) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (value != null) {
			pdp.addProperty("policy_decision_point", value);
		}
		config.add("pdp", pdp);
		env.putObject("config", config);
	}

	@Test
	public void validHttpsUrl_succeeds() {
		putIdentifier("https://pdp.example.com");
		cond.execute(env);
	}

	@Test
	public void missingIdentifier_fails() {
		putIdentifier(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void httpScheme_fails() {
		putIdentifier("http://pdp.example.com");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void withQuery_fails() {
		putIdentifier("https://pdp.example.com/path?foo=bar");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void withFragment_fails() {
		putIdentifier("https://pdp.example.com/path#frag");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
