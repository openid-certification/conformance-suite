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
public class EnsurePoppyGrantedScopeNotWiderThanRequested_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyGrantedScopeNotWiderThanRequested cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyGrantedScopeNotWiderThanRequested();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String json) {
		JsonObject client = new JsonObject();
		client.addProperty("scope", "orders:read");
		env.putObject("client", client);
		env.putObject("token_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void absentScopePasses() {
		assertDoesNotThrow(() -> run("{}"));
	}

	@Test
	public void sameScopePasses() {
		assertDoesNotThrow(() -> run("{\"scope\":\"orders:read\"}"));
	}

	@Test
	public void widerScopeFails() {
		ConditionError e = assertThrows(ConditionError.class, () -> run("{\"scope\":\"orders:read orders:write\"}"));
		assertTrue(e.getMessage().contains("orders:write"));
	}

	@Test
	public void emptyScopeFails() {
		assertThrows(ConditionError.class, () -> run("{\"scope\":\"\"}"));
	}
}
