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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsurePolicyDecisionPointMatchesIssuer_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePolicyDecisionPointMatchesIssuer cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePolicyDecisionPointMatchesIssuer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String configuredUrl, String returnedUrl) {
		JsonObject config = new JsonObject();
		JsonObject pdpCfg = new JsonObject();
		if (configuredUrl != null) {
			pdpCfg.addProperty("policy_decision_point", configuredUrl);
		}
		config.add("pdp", pdpCfg);
		env.putObject("config", config);

		JsonObject server = new JsonObject();
		if (returnedUrl != null) {
			server.addProperty("policy_decision_point", returnedUrl);
		}
		env.putObject("server", server);
	}

	@Test
	public void identicalUrls_succeeds() {
		put("https://pdp.example.com/", "https://pdp.example.com/");
		cond.execute(env);
	}

	@Test
	public void trailingSlashMismatch_fails() {
		// §9.2.3 requires identical match — trailing-slash normalization was
		// intentionally removed (commit f27f2772c). The caller stops the test on this, so the message
		// has to name the near-miss rather than leave the tester diffing two URLs by eye.
		put("https://pdp.example.com", "https://pdp.example.com/");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("differ only in a trailing '/'"), e.getMessage());
	}

	@Test
	public void caseMismatch_fails() {
		put("https://PDP.example.com", "https://pdp.example.com");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("differ only in case"), e.getMessage());
	}

	@Test
	public void whitespaceMismatch_fails() {
		put("https://pdp.example.com ", "https://pdp.example.com");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("surrounding whitespace"), e.getMessage());
	}

	@Test
	public void completeMismatch_fails() {
		put("https://pdp.example.com/", "https://other.example.com/");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingConfiguredUrl_fails() {
		put(null, "https://pdp.example.com/");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingReturnedUrl_fails() {
		put("https://pdp.example.com/", null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
