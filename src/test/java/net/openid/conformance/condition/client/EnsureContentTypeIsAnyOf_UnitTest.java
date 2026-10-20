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
public class EnsureContentTypeIsAnyOf_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureContentTypeIsAnyOf cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureContentTypeIsAnyOf("application/json", "Application/JWT");
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setHeader(Environment env, String value) {
		JsonObject headers = new JsonObject();
		if (value != null) {
			headers.addProperty("content-type", value);
		}
		JsonObject response = new JsonObject();
		response.add("headers", headers);
		env.putObject("endpoint_response", response);
	}

	@Test
	public void testEvaluate_firstAllowedType() {
		setHeader(env, "application/json");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_withCharset() {
		setHeader(env, "application/json   ; charset=UTF-8");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_mixedCaseHeader() {
		setHeader(env, "Application/JSON; charset=UTF-8");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_mixedCaseAllowedType() {
		// the allowed type was given as Application/JWT
		setHeader(env, "application/jwt");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_notAllowed() {
		setHeader(env, "text/html");

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
