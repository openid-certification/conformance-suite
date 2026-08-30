package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class UnexpectedHttpRequestReceived_UnitTest {

	private UnexpectedHttpRequestReceived cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new UnexpectedHttpRequestReceived();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_alwaysFailsReportingTheRequest() {
		JsonObject unexpected = new JsonObject();
		unexpected.addProperty("path", "requesturi/abc/extensions");
		unexpected.addProperty("method", "GET");
		env.putObject(UnexpectedHttpRequestReceived.ENV_KEY, unexpected);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'requesturi/abc/extensions'"), e.getMessage());
		assertTrue(e.getMessage().contains("does not serve"), e.getMessage());
	}
}
