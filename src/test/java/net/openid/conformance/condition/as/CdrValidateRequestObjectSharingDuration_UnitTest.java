package net.openid.conformance.condition.as;

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
public class CdrValidateRequestObjectSharingDuration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateRequestObjectSharingDuration cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateRequestObjectSharingDuration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addRequestObjectClaims(String claimsJson) {
		env.putObject("authorization_request_object",
			JsonParser.parseString("{\"claims\":{\"claims\":" + claimsJson + "}}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_number() {
		addRequestObjectClaims("{\"sharing_duration\":7776000}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_absent() {
		addRequestObjectClaims("{}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_notNumber() {
		assertThrows(ConditionError.class, () -> {
			addRequestObjectClaims("{\"sharing_duration\":\"7776000\"}");
			cond.execute(env);
		});
	}

}
