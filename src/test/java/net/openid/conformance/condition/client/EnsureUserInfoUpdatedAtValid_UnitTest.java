package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureUserInfoUpdatedAtValid_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureUserInfoUpdatedAtValid cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureUserInfoUpdatedAtValid();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_notPresent() {

		JsonObject userInfo = new JsonObject();

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_march2022() {

		JsonObject userInfo = new JsonObject();

		userInfo.addProperty("updated_at", 1648202173);

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_march2022Noninteger() {

		JsonObject userInfo = new JsonObject();

		// floats are allowed; OIDCC just defines it as a 'JSON number'
		userInfo.addProperty("updated_at", 1648202173.5);

		env.putObject("userinfo", userInfo);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_zero() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("updated_at", 0);

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidIsoWithTime() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("updated_at", "2000-01-01T00:00:00.000Z");

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidFutureYear2090() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("updated_at", 3794118953L);

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidPastYear1980() {
		assertThrows(ConditionError.class, () -> {

			JsonObject userInfo = new JsonObject();

			userInfo.addProperty("updated_at", 322822553);

			env.putObject("userinfo", userInfo);

			cond.execute(env);

		});

	}

}
