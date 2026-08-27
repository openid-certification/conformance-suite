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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrValidateAccessTokenExpiresIn_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateAccessTokenExpiresIn cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateAccessTokenExpiresIn();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void addExpiresIn(Object value) {
		JsonObject o = new JsonObject();
		if (value instanceof Number number) {
			o.addProperty("expires_in", number);
		} else {
			o.addProperty("expires_in", (String) value);
		}
		env.putObject("expires_in", o);
	}

	@Test
	public void testEvaluate_isGood() {
		addExpiresIn(300);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_boundsAreGood() {
		addExpiresIn(120);
		cond.execute(env);
		addExpiresIn(600);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_underTwoMinutes() {
		assertThrows(ConditionError.class, () -> {
			addExpiresIn(60);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_overTenMinutes() {
		assertThrows(ConditionError.class, () -> {
			addExpiresIn(700);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_isNotNumber() {
		assertThrows(ConditionError.class, () -> {
			addExpiresIn("300");
			cond.execute(env);
		});
	}

}
