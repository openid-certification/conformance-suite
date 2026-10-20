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

@ExtendWith(MockitoExtension.class)
class CheckPublishedPDPMetadataEndpoints_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckPublishedPDPMetadataEndpoints cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckPublishedPDPMetadataEndpoints();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void run(String server) {
		env.putObject("server", JsonParser.parseString(server).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void httpsEndpoints_succeed() {
		run("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "search_subject_endpoint": "https://pdp.example.com/access/v1/search/subject" }""");
	}

	@Test
	public void missingAccessEvaluationEndpoint_isNotRequired() {
		// It may be carried by the signed metadata; WarnPublishedMetadataOmitsAccessEvaluationEndpoint reports it.
		run("""
			{ "policy_decision_point": "https://pdp.example.com" }""");
	}

	@Test
	public void httpAccessEvaluationEndpoint_fails() {
		assertThrows(ConditionError.class, () -> run("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "http://pdp.example.com/access/v1/evaluation" }"""));
	}

	@Test
	public void httpSearchEndpoint_fails() {
		assertThrows(ConditionError.class, () -> run("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "search_action_endpoint": "http://pdp.example.com/access/v1/search/action" }"""));
	}

	@Test
	public void nonStringEndpoint_fails() {
		assertThrows(ConditionError.class, () -> run("""
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluations_endpoint": 42 }"""));
	}

	@Test
	public void missingPolicyDecisionPoint_fails() {
		assertThrows(ConditionError.class, () -> run("""
			{ "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }"""));
	}
}
