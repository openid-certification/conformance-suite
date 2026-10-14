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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CheckStaticPDPServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void run(Condition cond, String server) {
		((AbstractCondition) cond).setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("server", JsonParser.parseString(server).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void static_identifierOnly_succeeds() {
		run(new CheckStaticPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com" }""");
	}

	@Test
	public void static_httpEndpoint_failsNamingTheField() {
		ConditionError e = assertThrows(ConditionError.class, () -> run(new CheckStaticPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com",
			  "search_action_endpoint": "http://pdp.example.com/access/v1/search/action" }"""));
		assertTrue(e.getMessage().contains("'Action Entity Search API Endpoint' field in the 'AuthZEN' section of the test configuration must use the https scheme"), e.getMessage());
	}

	@Test
	public void static_invalidUrl_failsNamingTheField() {
		ConditionError e = assertThrows(ConditionError.class, () -> run(new CheckStaticPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluation_endpoint": "not a url" }"""));
		assertTrue(e.getMessage().contains("'Access Evaluation API Endpoint' field in the 'AuthZEN' section of the test configuration is not a valid URL"), e.getMessage());
	}

	@Test
	public void static_emptyEndpoint_isNotChecked() {
		// A cleared form field stays in the configuration as an empty string; the module then
		// calls the default path, so there is no URL to check.
		run(new CheckStaticPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluations_endpoint": "",
			  "search_subject_endpoint": null }""");
	}

	@Test
	public void static_emptyEndpoint_doesNotHideAnInvalidOne() {
		assertThrows(ConditionError.class, () -> run(new CheckStaticPDPServerConfiguration(), """
			{ "policy_decision_point": "https://pdp.example.com",
			  "access_evaluations_endpoint": "",
			  "search_action_endpoint": "http://pdp.example.com/access/v1/search/action" }"""));
	}

	@Test
	public void static_emptyIdentifier_fails() {
		assertThrows(ConditionError.class, () -> run(new CheckStaticPDPServerConfiguration(), """
			{ "policy_decision_point": "" }"""));
	}

	@Test
	public void static_missingIdentifier_failsNamingTheField() {
		ConditionError e = assertThrows(ConditionError.class, () -> run(new CheckStaticPDPServerConfiguration(), """
			{ "access_evaluation_endpoint": "https://pdp.example.com/access/v1/evaluation" }"""));
		assertTrue(e.getMessage().contains("'PDP Identifier' field in the 'AuthZEN' section of the test configuration is missing"), e.getMessage());
	}
}
