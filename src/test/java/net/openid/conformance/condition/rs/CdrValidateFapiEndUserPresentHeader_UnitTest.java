package net.openid.conformance.condition.rs;

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
public class CdrValidateFapiEndUserPresentHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateFapiEndUserPresentHeader cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateFapiEndUserPresentHeader();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addIncomingRequestWithHeader(String value) {
		String headers = value == null ? "{}" : "{\"x-fapi-end-user-present\":\"" + value + "\"}";
		env.putObject("incoming_request", JsonParser.parseString("{\"headers\":" + headers + "}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_true() {
		addIncomingRequestWithHeader("true");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_false() {
		addIncomingRequestWithHeader("false");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {
			addIncomingRequestWithHeader(null);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidValue() {
		assertThrows(ConditionError.class, () -> {
			addIncomingRequestWithHeader("TRUE");
			cond.execute(env);
		});
	}

}
