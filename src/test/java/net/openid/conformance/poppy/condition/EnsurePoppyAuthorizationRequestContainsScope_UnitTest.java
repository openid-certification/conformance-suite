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
public class EnsurePoppyAuthorizationRequestContainsScope_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyAuthorizationRequestContainsScope cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyAuthorizationRequestContainsScope();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String json) {
		env.putObject("effective_authorization_endpoint_request", JsonParser.parseString(json).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void definedScopePasses() {
		assertDoesNotThrow(() -> run("{\"scope\":\"orders:read\"}"));
	}

	@Test
	public void missingScopeFails() {
		assertThrows(ConditionError.class, () -> run("{}"));
	}

	@Test
	public void emptyScopeFails() {
		assertThrows(ConditionError.class, () -> run("{\"scope\":\"\"}"));
	}

	@Test
	public void undefinedScopeFailsWithInvalidScope() {
		ConditionError e = assertThrows(ConditionError.class, () -> run("{\"scope\":\"orders:read orders:write\"}"));
		assertTrue(e.getMessage().contains("invalid_scope"));
	}
}
