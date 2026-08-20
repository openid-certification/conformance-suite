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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ParseVical_UnitTest {

	private ParseVical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ParseVical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putVical(byte[] signedVical) {
		VicalTestFixtures.putVical(env, signedVical);
	}

	@Test
	public void testEvaluate_parsesValidVical() {
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		byte[] signedVical = VicalTestFixtures.goodSignedVical(List.of(iaca.getCert()));
		putVical(signedVical);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsOnGarbage() {
		putVical(new byte[] { 0x01, 0x02, 0x03 });

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("parse"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnInvalidBase64() {
		JsonObject vical = new JsonObject();
		vical.addProperty("value", "not!!!base64");
		env.putObject("vical", vical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("base64"), e.getMessage());
	}
}
