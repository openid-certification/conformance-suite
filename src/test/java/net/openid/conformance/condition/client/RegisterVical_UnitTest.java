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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class RegisterVical_UnitTest {

	private RegisterVical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new RegisterVical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putConfig(String vicalB64, String vicalUrl) {
		JsonObject credential = new JsonObject();
		if (vicalB64 != null) {
			credential.addProperty("vical", vicalB64);
		}
		if (vicalUrl != null) {
			credential.addProperty("vical_url", vicalUrl);
		}
		JsonObject config = new JsonObject();
		config.add("credential", credential);
		env.putObject("config", config);
	}

	@Test
	public void testEvaluate_registersInlineVical() {
		putConfig("dGVzdA==", null);

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals("dGVzdA==", env.getString("vical", "value"));
		assertNull(env.getString("vical_url"));
	}

	@Test
	public void testEvaluate_registersVicalUrl() {
		putConfig(null, "https://vical.example.com/vical.cbor");

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals("https://vical.example.com/vical.cbor", env.getString("vical_url"));
		assertNull(env.getObject("vical"));
	}

	@Test
	public void testEvaluate_errorsWhenVicalUrlIsNotHttps() {
		putConfig(null, "http://vical.example.com/vical.cbor");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("https"), e.getMessage());
	}

	@Test
	public void testEvaluate_errorsWhenBothConfigured() {
		putConfig("dGVzdA==", "https://vical.example.com/vical.cbor");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("VICAL"), e.getMessage());
	}

	@Test
	public void testEvaluate_skipsWhenFieldsAreEmptyStrings() {
		// the config form keeps cleared fields as "" rather than removing them
		putConfig("", "");

		assertDoesNotThrow(() -> cond.execute(env));

		assertNull(env.getObject("vical"));
		assertNull(env.getString("vical_url"));
	}

	@Test
	public void testEvaluate_skipsWhenNeitherConfigured() {
		putConfig(null, null);

		assertDoesNotThrow(() -> cond.execute(env));

		assertNull(env.getObject("vical"));
		assertNull(env.getString("vical_url"));
	}
}
