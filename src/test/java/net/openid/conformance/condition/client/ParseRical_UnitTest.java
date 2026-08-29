package net.openid.conformance.condition.client;

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
public class ParseRical_UnitTest {

	private ParseRical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ParseRical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_parsesGoodRical() {
		RicalTestFixtures.ReaderPki pki = RicalTestFixtures.generateReaderPki();
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsOnGarbage() {
		RicalTestFixtures.putRical(env, new byte[] { 0x01, 0x02, 0x03 });

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("parse"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsOnInvalidBase64() {
		com.google.gson.JsonObject rical = new com.google.gson.JsonObject();
		rical.addProperty("value", "not!!base64");
		env.putObject("rical", rical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("base64"), e.getMessage());
	}
}
