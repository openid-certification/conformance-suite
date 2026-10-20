package net.openid.conformance.authzen.condition;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WarnPublishedMetadataOmitsAccessEvaluationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnPublishedMetadataOmitsAccessEvaluationEndpoint cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnPublishedMetadataOmitsAccessEvaluationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	@Test
	public void present_succeeds() {
		env.putObject("server", JsonParser.parseString("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }""").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void absent_isReported() {
		env.putObject("server", JsonParser.parseString("""
			{ "policy_decision_point": "https://pdp.example.com", "signed_metadata": "a.b.c" }""").getAsJsonObject());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("has no access_evaluation_endpoint"), e.getMessage());
	}
}
