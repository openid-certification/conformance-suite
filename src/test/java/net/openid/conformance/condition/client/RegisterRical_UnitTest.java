package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class RegisterRical_UnitTest {

	private RegisterRical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new RegisterRical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putConfig(String rical, String ricalUrl) {
		JsonObject client = new JsonObject();
		if (rical != null) {
			client.addProperty("rical", rical);
		}
		if (ricalUrl != null) {
			client.addProperty("rical_url", ricalUrl);
		}
		JsonObject config = new JsonObject();
		config.add("client", client);
		env.putObject("config", config);
	}

	@Test
	public void testEvaluate_registersInlineRical() {
		putConfig("dGVzdA==", null);

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals("dGVzdA==", env.getString("rical", "value"));
		assertNull(env.getString("rical_url"));
	}

	@Test
	public void testEvaluate_registersRicalUrl() {
		putConfig(null, "https://rical.example.com/rical.cbor");

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals("https://rical.example.com/rical.cbor", env.getString("rical_url"));
		assertFalse(env.containsObject("rical"));
	}

	@Test
	public void testEvaluate_noopWhenNothingConfigured() {
		putConfig(null, null);

		assertDoesNotThrow(() -> cond.execute(env));

		assertFalse(env.containsObject("rical"));
		assertNull(env.getString("rical_url"));
	}

	@Test
	public void testEvaluate_treatsBlankFieldsAsAbsent() {
		putConfig("", "");

		assertDoesNotThrow(() -> cond.execute(env));

		assertFalse(env.containsObject("rical"));
		assertNull(env.getString("rical_url"));
	}

	@Test
	public void testEvaluate_failsWhenBothConfigured() {
		putConfig("dGVzdA==", "https://rical.example.com/rical.cbor");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Only one"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnPlainHttpUrl() {
		putConfig(null, "http://rical.example.com/rical.cbor");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("https"), e.getMessage());
	}
}
