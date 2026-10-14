package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CheckPDPServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void run(Condition cond, String server) {
		((AbstractCondition) cond).setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("server", JsonParser.parseString(server).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void discovered_missingAccessEvaluationEndpoint_fails() {
		// Section 9.1.1 makes access_evaluation_endpoint REQUIRED in the PDP metadata.
		assertThrows(ConditionError.class, () -> run(new CheckPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com" }"""));
	}

	@Test
	public void discovered_emptyOptionalEndpoint_fails() {
		// In PDP metadata an empty endpoint is a defect of the PDP, not a field left blank.
		assertThrows(ConditionError.class, () -> run(new CheckPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation",
			  "access_evaluations_endpoint": "" }"""));
	}

	@Test
	public void discovered_requiredEndpoints_succeed() {
		run(new CheckPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }""");
	}
}
