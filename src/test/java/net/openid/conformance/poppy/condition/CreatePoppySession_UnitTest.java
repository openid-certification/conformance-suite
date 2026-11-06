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
public class CreatePoppySession_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreatePoppySession cond;

	@BeforeEach
	public void setUp() {
		cond = new CreatePoppySession();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void producesIds() {
		cond.execute(env);
		String sid = env.getString("poppy_sid");
		assertTrue(sid.matches("[A-Za-z0-9_-]{22}"));
		assertTrue(env.getString("poppy_user_id").startsWith("usr_"));
		assertTrue(env.getString("poppy_user_id").length() >= 10);
		cond.execute(env);
		assertNotEquals(sid, env.getString("poppy_sid"));
	}
}
