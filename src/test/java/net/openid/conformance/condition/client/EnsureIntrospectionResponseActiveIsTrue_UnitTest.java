package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureIntrospectionResponseActiveIsTrue_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureIntrospectionResponseActiveIsTrue cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIntrospectionResponseActiveIsTrue();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addResponse(String bodyJson) {
		env.putObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY,
			JsonParser.parseString("{\"body_json\":" + bodyJson + "}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_active() {
		addResponse("{\"active\":true}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_inactive() {
		assertThrows(ConditionError.class, () -> {
			addResponse("{\"active\":false}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {
			addResponse("{}");
			cond.execute(env);
		});
	}

}
