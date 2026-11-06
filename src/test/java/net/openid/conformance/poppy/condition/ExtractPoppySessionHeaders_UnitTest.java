package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ExtractPoppySessionHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractPoppySessionHeaders cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractPoppySessionHeaders();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String headers) {
		env.putObject("incoming_request", JsonParser.parseString("{\"headers\":" + headers + "}").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void copiesBothHeaders() {
		run("{\"poppy-session\":\"a.b.c\",\"poppy-version\":\"0.4\"}");
		assertEquals("a.b.c", env.getString("poppy_session_jwt"));
		assertEquals("0.4", env.getString("poppy_version"));
	}

	@Test
	public void missingSessionFailsNamingInvalidSession() {
		ConditionError e = assertThrows(ConditionError.class, () -> run("{\"poppy-version\":\"0.4\"}"));
		assertTrue(e.getMessage().contains("invalid_session"));
	}

	@Test
	public void missingVersionFailsNamingVersionUnsupported() {
		ConditionError e = assertThrows(ConditionError.class, () -> run("{\"poppy-session\":\"a.b.c\"}"));
		assertTrue(e.getMessage().contains("version_unsupported"));
	}
}
