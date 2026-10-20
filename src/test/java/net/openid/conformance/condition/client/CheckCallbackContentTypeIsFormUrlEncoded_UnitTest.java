package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckCallbackContentTypeIsFormUrlEncoded_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckCallbackContentTypeIsFormUrlEncoded cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckCallbackContentTypeIsFormUrlEncoded();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setHeader(Environment env, String value) {
		JsonObject headers = new JsonObject();
		if (value != null) {
			headers.addProperty("content-type", value);
		}
		env.putObject("callback_headers", headers);
	}

	@Test
	public void testEvaluate_noError() {
		setHeader(env, "application/x-www-form-urlencoded");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_withCharset() {
		setHeader(env, "application/x-www-form-urlencoded; charset=UTF-8");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_mixedCase() {
		setHeader(env, "Application/X-WWW-Form-URLEncoded");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalid() {
		setHeader(env, "application/json");

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missing() {
		setHeader(env, null);

		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}
}
