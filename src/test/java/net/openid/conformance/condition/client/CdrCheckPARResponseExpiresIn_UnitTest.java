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
public class CdrCheckPARResponseExpiresIn_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrCheckPARResponseExpiresIn cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrCheckPARResponseExpiresIn();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void addParResponse(String expiresInJson) {
		env.putObject(CallPAREndpoint.RESPONSE_KEY,
			JsonParser.parseString("{\"body_json\":" + expiresInJson + "}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_isGood() {
		addParResponse("{\"expires_in\":90}");
		cond.execute(env);
		addParResponse("{\"expires_in\":10}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_tooShort() {
		assertThrows(ConditionError.class, () -> {
			addParResponse("{\"expires_in\":5}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_tooLong() {
		assertThrows(ConditionError.class, () -> {
			addParResponse("{\"expires_in\":3600}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {
			addParResponse("{}");
			cond.execute(env);
		});
	}

}
