package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
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
public class CheckDiscRequirePushedAuthorizationRequestsIsTrue_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckDiscRequirePushedAuthorizationRequestsIsTrue cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckDiscRequirePushedAuthorizationRequestsIsTrue();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isTrue() {
		env.putObject("server", JsonParser.parseString("{\"require_pushed_authorization_requests\":true}").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_isFalse() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("server", JsonParser.parseString("{\"require_pushed_authorization_requests\":false}").getAsJsonObject());
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("server", JsonParser.parseString("{}").getAsJsonObject());
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notBoolean() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("server", JsonParser.parseString("{\"require_pushed_authorization_requests\":\"true\"}").getAsJsonObject());
			cond.execute(env);
		});
	}

}
